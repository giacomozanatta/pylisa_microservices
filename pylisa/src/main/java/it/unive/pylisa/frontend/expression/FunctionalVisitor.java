package it.unive.pylisa.frontend.expression;

import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.pylisa.antlr.Python3Parser.Comp_forContext;
import it.unive.pylisa.antlr.Python3Parser.Comp_ifContext;
import it.unive.pylisa.antlr.Python3Parser.Comp_iterContext;
import it.unive.pylisa.antlr.Python3Parser.LambdefContext;
import it.unive.pylisa.antlr.Python3Parser.Lambdef_nocondContext;
import it.unive.pylisa.antlr.Python3Parser.Namedexpr_testContext;
import it.unive.pylisa.antlr.Python3Parser.TestContext;
import it.unive.pylisa.antlr.Python3Parser.Test_nocondContext;
import it.unive.pylisa.antlr.Python3Parser.VarargslistContext;
import it.unive.pylisa.antlr.Python3Parser.VfpdefContext;
import it.unive.pylisa.cfg.expression.LambdaExpression;
import it.unive.pylisa.cfg.expression.PyAssign;
import it.unive.pylisa.cfg.expression.PyTernaryOperator;
import it.unive.pylisa.frontend.ParserContext;
import it.unive.pylisa.frontend.ParserSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Handles functional-flavour expressions: the ternary operator form of
 * {@code test}, {@code lambdef}, and the currently-unsupported comprehension
 * nodes. Extracted from {@link ExpressionVisitor} in Chunk 3.
 */
public final class FunctionalVisitor {

	private final ParserContext ctx;
	private final ParserSupport support;

	public FunctionalVisitor(
			ParserContext ctx,
			ParserSupport support) {
		this.ctx = Objects.requireNonNull(ctx);
		this.support = Objects.requireNonNull(support);
	}

	/**
	 * Desugars a {@code namedexpr_test} (walrus, PEP 572). When no {@code :=}
	 * is present the rule reduces to a plain {@code test}. For the walrus form
	 * {@code (x := rhs)}, a {@link PyAssign} is emitted into the caller's
	 * walrus-prelude frame (see {@link ParserContext#beginWalrusPrelude()}) and
	 * this method returns a {@link VariableRef} to the target. Reusing
	 * {@link PyAssign} keeps the side-effect semantics identical to an
	 * explicit {@code x = rhs} assignment; the caller threads the prelude
	 * into its CFG block before the guard node that consumes the value.
	 * <p>
	 * If no prelude frame is active the walrus cannot be hoisted cleanly, so
	 * the construct is rejected via {@code support.rejectUnsupported} instead
	 * of silently dropping the side-effect.
	 */
	public Expression visitNamedexpr_test(
			Namedexpr_testContext pctx) {
		if (pctx.COLONEQ() == null)
			return visitTest(pctx.test(0));

		if (!ctx.hasActiveWalrusPrelude())
			return support.rejectUnsupported(pctx, ":= walrus in unsupported position");

		// Mirror SimpleStatementVisitor.visitExpr_stmt: for the walrus LHS we
		// want a raw VariableRef, not a scoped unit-prepended access, so the
		// subsequent scopeAssignmentTarget() can make the correct decision
		// based on the current scope frames.
		boolean prevPrepend = ctx.shouldPrependUnitAccess();
		ctx.shouldPrependUnitAccess(false);
		Expression lhsExpr;
		try {
			lhsExpr = visitTest(pctx.test(0));
		} finally {
			ctx.shouldPrependUnitAccess(prevPrepend);
		}
		if (!(lhsExpr instanceof VariableRef targetVar))
			return support.rejectUnsupported(pctx, ":= walrus with non-identifier LHS");

		Expression rhs = visitTest(pctx.test(1));
		ctx.stmt().simple().declareAssignedNames(targetVar);
		Expression scopedTarget = ctx.stmt().simple().scopeAssignmentTarget(targetVar);
		ctx.addWalrusPrelude(new PyAssign(ctx.currentCFG(), support.getLocation(pctx), scopedTarget, rhs));

		return new VariableRef(ctx.currentCFG(), support.getLocation(pctx), targetVar.getName());
	}

	public Expression visitTest(
			TestContext pctx) {
		if (pctx.IF() != null) {
			Expression trueCase = ctx.expr().visitOr_test(pctx.or_test(0));
			Expression booleanGuard = ctx.expr().visitOr_test(pctx.or_test(1));
			Expression falseCase = visitTest(pctx.test());
			return new PyTernaryOperator(ctx.currentCFG(), support.getLocation(pctx), booleanGuard,
					trueCase, falseCase);
		}
		if (pctx.lambdef() != null)
			return visitLambdef(pctx.lambdef());
		return ctx.expr().visitOr_test(pctx.or_test(0));
	}

	public Expression visitTest_nocond(
			Test_nocondContext pctx) {
		if (pctx.or_test() != null)
			return ctx.expr().visitOr_test(pctx.or_test());
		return visitLambdef_nocond(pctx.lambdef_nocond());
	}

	public Expression visitLambdef(
			LambdefContext pctx) {
		List<Expression> args = pctx.varargslist() != null
				? extractNamesFromVarArgList(pctx.varargslist())
				: new ArrayList<>();
		Expression body = visitTest(pctx.test());
		return new LambdaExpression(args, body, ctx.currentCFG(), support.getLocation(pctx));
	}

	public Expression visitLambdef_nocond(
			Lambdef_nocondContext pctx) {
		List<Expression> args = pctx.varargslist() != null
				? extractNamesFromVarArgList(pctx.varargslist())
				: new ArrayList<>();
		Expression body = visitTest_nocond(pctx.test_nocond());
		return new LambdaExpression(args, body, ctx.currentCFG(), support.getLocation(pctx));
	}

	public Object visitComp_iter(
			Comp_iterContext pctx) {
		return support.rejectUnsupported(pctx);
	}

	public Object visitComp_for(
			Comp_forContext pctx) {
		return support.rejectUnsupported(pctx);
	}

	public Object visitComp_if(
			Comp_ifContext pctx) {
		return support.rejectUnsupported(pctx);
	}

	private List<Expression> extractNamesFromVarArgList(
			VarargslistContext varargslist) {
		List<VfpdefContext> names = varargslist.vfpdef();
		List<Expression> result = new ArrayList<>();
		if (names.size() == 0)
			return result;
		for (VfpdefContext e : names)
			result.add(new VariableRef(ctx.currentCFG(), support.getLocation(e),
					ctx.def().visitVfpdef(e)));
		return result;
	}
}
