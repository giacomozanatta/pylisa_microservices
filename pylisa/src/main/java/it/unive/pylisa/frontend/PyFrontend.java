package it.unive.pylisa.frontend;

import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.frontend.LiSAFrontend;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.pylisa.PythonFeatures;
import it.unive.pylisa.PythonTypeSystem;
import it.unive.pylisa.cfg.type.PyClassType;
import it.unive.pylisa.cfg.type.PyFunctionType;
import it.unive.pylisa.cfg.type.PyModuleType;
import it.unive.pylisa.frontend.definition.DefinitionVisitor;
import it.unive.pylisa.frontend.expression.ExpressionVisitor;
import it.unive.pylisa.frontend.statement.StatementVisitor;
import it.unive.pylisa.program.ModuleUnit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Public entry point to the PyLiSA front-end: parses a Python source file (or
 * Jupyter notebook) into a LiSA {@link Program}.
 * <p>
 * Post-Chunk-7 this class is a thin orchestrator. Heavy-lifting is delegated to
 * {@link AntlrPipeline} (lexer + parser), {@link ProgramFinalizer} (type
 * registration + entry-point CFG), and {@link ModuleLoaderCallback} (recursive
 * sub-module parsing). The shared {@link ParserContext} holds mutable parse
 * state; {@link ParserSupport} holds stateless helpers; the three category
 * visitors (expression / statement / definition) translate Python AST into LiSA
 * IR.
 */
public final class PyFrontend implements LiSAFrontend {

	private static final Logger LOG = LogManager.getLogger(PyFrontend.class);

	private final ParserContext ctx;
	private final ParserSupport support;
	private final ExpressionVisitor expr;
	private final StatementVisitor stmt;
	private final DefinitionVisitor def;
	private final ModuleLoaderCallback moduleLoader;

	private final String filePath;
	private final boolean notebook;
	private final List<Integer> cellOrder;
	private final boolean strict;

	private final CFG init;

	public PyFrontend(
			String filePath,
			boolean notebook) {
		this(filePath, notebook, Collections.emptyList(), null, false);
	}

	public PyFrontend(
			String filePath,
			boolean notebook,
			Integer... cellOrder) {
		this(filePath, notebook, List.of(cellOrder), null, false);
	}

	public PyFrontend(
			String filePath,
			boolean notebook,
			List<Integer> cellOrder) {
		this(filePath, notebook, cellOrder, null, false);
	}

	public PyFrontend(
			String filePath,
			boolean notebook,
			String sourceRoot) {
		this(filePath, notebook, Collections.emptyList(), sourceRoot, false);
	}

	public PyFrontend(
			String filePath,
			boolean notebook,
			List<Integer> cellOrder,
			String sourceRoot) {
		this(filePath, notebook, cellOrder, sourceRoot, false);
	}

	/**
	 * Returns a {@link PyFrontend} in strict mode: any {@code UNSOUND}
	 * diagnostic emitted during parsing is promoted to a
	 * {@link DiagnosticReporter.StrictModeViolation}. Useful for regression
	 * tests that want to guarantee no silent approximations occur.
	 */
	public static PyFrontend strict(
			String filePath) {
		return new PyFrontend(filePath, false, Collections.emptyList(), null, true);
	}

	public PyFrontend(
			String filePath,
			boolean notebook,
			List<Integer> cellOrder,
			String sourceRoot,
			boolean strict) {
		this.filePath = filePath;
		this.notebook = notebook;
		this.cellOrder = cellOrder;
		this.strict = strict;

		this.ctx = new ParserContext();
		this.ctx.reporter(new DiagnosticReporter(strict));
		this.ctx.filePath(filePath);
		this.ctx.currentFileIsPackage(filePath != null && filePath.endsWith("__init__.py"));

		Program program = new Program(new PythonFeatures(), new PythonTypeSystem());
		this.ctx.program(program);
		ModuleUnit mainModule = new ModuleUnit(new SourceCodeLocation(filePath, 0, 0), program, "__main__");
		this.ctx.currentModule(mainModule);
		this.ctx.currentUnit(mainModule);
		this.init = makeInit(program);
		this.ctx.init(this.init);
		Path baseDir = (sourceRoot != null)
				? Path.of(sourceRoot)
				: (filePath != null) ? Path.of(filePath).getParent() : Path.of(".");
		this.ctx.importManager(new PythonModuleImportManager(program, init, baseDir));
		program.addUnit(mainModule);
		PyModuleType.register("__main__", mainModule);

		this.support = new ParserSupport(ctx);
		this.expr = new ExpressionVisitor(ctx, support);
		this.stmt = new StatementVisitor(ctx, support);
		this.def = new DefinitionVisitor(ctx, support);
		this.ctx.wireVisitors(expr, stmt, def);
		this.moduleLoader = new ModuleLoaderCallback(ctx, support, stmt);
		LOG.debug("PyFrontend wired for {}", filePath);
	}

	public PyFrontend setContinueOnUnsupportedStatement(
			boolean value) {
		this.ctx.continueOnUnsupportedStatement(value);
		return this;
	}

	public String getFilePath() {
		return filePath;
	}

	/**
	 * Returns the diagnostic reporter associated with this frontend instance.
	 * Tests and tooling can inspect {@link DiagnosticReporter#events()} after
	 * {@link #toLiSAProgram(boolean)} completes to see what the parse produced.
	 */
	public DiagnosticReporter reporter() {
		return ctx.reporter();
	}

	/**
	 * Returns {@code true} iff this frontend was constructed in strict mode.
	 */
	public boolean strict() {
		return strict;
	}

	@Override
	public Program toLiSAProgram() throws IOException, AnalysisSetupException {
		return toLiSAProgram(true);
	}

	public Program toLiSAProgram(
			boolean clearClassType)
			throws IOException,
			AnalysisSetupException {
		if (clearClassType)
			resetTypeRegistries();

		ProgramFinalizer finalizer = new ProgramFinalizer(ctx, init);
		finalizer.prepare();

		LOG.info("reading {}", filePath);
		ctx.importManager().setProjectLoader(moduleLoader::load);

		String source;
		try {
			source = new SourceReader(filePath, notebook, cellOrder).readNormalizedSource();
		} catch (IOException e) {
			throw new IOException("Unable to parse '" + filePath + "'", e);
		}
		stmt.visitFile_input(new AntlrPipeline(filePath, source).parseFile());

		return finalizer.finalizeProgram();
	}

	public ModuleUnit loadProjectModuleFile(
			String moduleName,
			String modulePath)
			throws IOException {
		return moduleLoader.load(moduleName, modulePath);
	}

	private void resetTypeRegistries() {
		PyClassType.clearAll();
		PyFunctionType.clearAll();
		PyModuleType.clearAll();
		PyModuleType.register(ctx.currentModule().getName(), ctx.currentModule());
	}

	private static CFG makeInit(
			Program program) {
		CFG init = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, program, false, "LiSA$init"));
		init.addNode(new Ret(init, SyntheticLocation.INSTANCE), true);
		program.addCodeMember(init);
		return init;
	}
}
