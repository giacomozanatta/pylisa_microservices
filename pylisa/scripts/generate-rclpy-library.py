#!/usr/bin/env python3
"""Generate pylisa's library specification of rclpy from the rclpy sources.

Usage: scripts/generate-rclpy-library.py <rclpy-package-dir> <implementations.txt> <output>

<rclpy-package-dir> is the `rclpy/rclpy` directory of an rclpy checkout (the
distribution the models target). For every class and module function listed in
LAYOUT, the generated specification declares the public callables with exactly
the parameters of the sources: positional parameters, keyword-only parameters,
`*args`, `**kwargs` and literal default values. Non-literal defaults become
`none`: the models apply the real default themselves.

Properties are not declared: in the analysed program they are plain attribute
reads of fields written by the models.

<implementations.txt> maps callables to the native classes that model them, one
`<module>[.<Class>].<callable> <native class>` per line; callables without an
entry are bound to DEFAULT_IMPLEMENTATION.
"""

import ast
import sys
from pathlib import Path

DEFAULT_IMPLEMENTATION = "it.unive.pylisa.libraries.rclpy.RosUnknownResult"
UNTYPED = "type it.unive.lisa.type.Untyped::INSTANCE"

# library module -> (source file, [classes], [module functions], [imported library modules])
LAYOUT = {
    "rclpy.context": ("context.py", ["Context"], [], []),
    "rclpy.qos": ("qos.py", ["QoSProfile"], [], []),
    "rclpy.task": ("task.py", ["Future"], [], []),
    "rclpy.parameter": ("parameter.py", ["Parameter"], [], []),
    "rclpy.clock": ("clock.py", ["Clock"], [], []),
    "rclpy.impl.rcutils_logger": ("impl/rcutils_logger.py", ["RcutilsLogger"], [], []),
    "rclpy.publisher": ("publisher.py", ["Publisher"], [], []),
    "rclpy.subscription": ("subscription.py", ["Subscription"], [], []),
    "rclpy.timer": ("timer.py", ["Timer", "Rate"], [], []),
    "rclpy.client": ("client.py", ["Client"], [], ["rclpy.task"]),
    "rclpy.service": ("service.py", ["Service"], [], []),
    "rclpy.guard_condition": ("guard_condition.py", ["GuardCondition"], [], []),
    "rclpy.executors": (
        "executors.py",
        ["Executor", "SingleThreadedExecutor", "MultiThreadedExecutor"],
        [],
        ["rclpy.task"],
    ),
    "rclpy.node": (
        "node.py",
        ["Node"],
        [],
        [
            "rclpy.context",
            "rclpy.qos",
            "rclpy.task",
            "rclpy.parameter",
            "rclpy.clock",
            "rclpy.impl.rcutils_logger",
            "rclpy.publisher",
            "rclpy.subscription",
            "rclpy.timer",
            "rclpy.client",
            "rclpy.service",
            "rclpy.guard_condition",
        ],
    ),
    "rclpy": (
        "__init__.py",
        [],
        ["init", "shutdown", "create_node", "spin_once", "spin", "spin_until_future_complete",
         "get_global_executor"],
        ["rclpy.context", "rclpy.node", "rclpy.executors", "rclpy.task", "rclpy.parameter"],
    ),
}

# module functions re-exported by a library module from another source file
REEXPORTED = {"rclpy": [("utilities.py", ["ok", "try_shutdown"])]}


def literal(node):
    """The specification literal for a default value, or `none`."""
    if isinstance(node, ast.Constant):
        value = node.value
        if value is None:
            return "none"
        if isinstance(value, bool):
            return "true" if value else "false"
        if isinstance(value, int) and value >= 0:
            return str(value)
        if isinstance(value, str) and '"' not in value and "\\" not in value:
            return '"' + value + '"'
    return "none"


def parameters(function, receiver):
    """The `param` lines of a function, in declaration order."""
    args = function.args
    lines = []
    positional = args.posonlyargs + args.args
    defaults = [None] * (len(positional) - len(args.defaults)) + list(args.defaults)
    for index, (arg, default) in enumerate(zip(positional, defaults)):
        if index == 0 and receiver is not None:
            lines.append(f"param {arg.arg} libtype {receiver}*")
            continue
        suffix = "" if default is None else f" default {literal(default)}"
        lines.append(f"param {arg.arg} {UNTYPED}{suffix}")
    if args.vararg is not None:
        lines.append(f"param *{args.vararg.arg} {UNTYPED}")
    for arg, default in zip(args.kwonlyargs, args.kw_defaults):
        suffix = "" if default is None else f" default {literal(default)}"
        lines.append(f"param &{arg.arg} {UNTYPED}{suffix}")
    if args.kwarg is not None:
        lines.append(f"param **{args.kwarg.arg} {UNTYPED}")
    return lines


def is_property(function):
    return any(
        (isinstance(d, ast.Name) and d.id == "property")
        or (isinstance(d, ast.Attribute) and d.attr in ("setter", "getter", "deleter"))
        for d in function.decorator_list
    )


def public_methods(cls):
    for node in cls.body:
        if not isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)) or is_property(node):
            continue
        if node.name == "__init__" or not node.name.startswith("_"):
            yield node


def method_block(indent, keyword, name, implementation, params):
    lines = [f"{indent}{keyword} {name}: {implementation}", f"{indent}    {UNTYPED}"]
    lines += [f"{indent}    {p}" for p in params]
    return lines


def main(package, implementations_file, output):
    implementations = {}
    for line in Path(implementations_file).read_text().splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            callable_name, native = line.split()
            implementations[callable_name] = native

    out = [
        "# rclpy (ROS 2 Humble), generated by scripts/generate-rclpy-library.py from the rclpy",
        "# sources: do not edit by hand. Signatures are those of the sources; the semantics of",
        "# each callable is given by the native class it is bound to.",
        "",
    ]
    for module, (source, classes, functions, imports) in LAYOUT.items():
        tree = ast.parse((Path(package) / source).read_text())
        definitions = {n.name: n for n in tree.body if isinstance(n, (ast.ClassDef, ast.FunctionDef))}
        out.append(f"library {module}:")
        out.append(f"    location {module}")
        out += [f"    imports {i}" for i in imports]
        extra = []
        for other_source, names in REEXPORTED.get(module, []):
            other = ast.parse((Path(package) / other_source).read_text())
            extra += [n for n in other.body if isinstance(n, ast.FunctionDef) and n.name in names]
        for function in [definitions[f] for f in functions] + extra:
            key = f"{module}.{function.name}"
            out += method_block("    ", "method", function.name,
                                implementations.get(key, DEFAULT_IMPLEMENTATION), parameters(function, None))
        for class_name in classes:
            cls = definitions[class_name]
            bases = [b.id for b in cls.bases if isinstance(b, ast.Name) and b.id in classes]
            base = f"{module}.{bases[0]}" if bases else "builtins.object"
            out.append(f"    class {class_name} extends {base}:")
            for method in public_methods(cls):
                key = f"{module}.{class_name}.{method.name}"
                out += method_block("        ", "instance method", method.name,
                                    implementations.get(key, DEFAULT_IMPLEMENTATION),
                                    parameters(method, class_name))
        out.append("")
    Path(output).write_text("\n".join(out))


if __name__ == "__main__":
    if len(sys.argv) != 4:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2], sys.argv[3])
