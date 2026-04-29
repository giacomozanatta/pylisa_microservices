package it.unive.pylisa.frontend.statement;

import it.unive.lisa.program.Program;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.NoOp;
import it.unive.lisa.program.cfg.statement.Ret;
import it.unive.lisa.program.cfg.statement.Return;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.evaluation.LeftToRightEvaluation;
import it.unive.pylisa.antlr.Python3Parser.Break_stmtContext;
import it.unive.pylisa.antlr.Python3Parser.Continue_stmtContext;
import it.unive.pylisa.antlr.Python3Parser.Flow_stmtContext;
import it.unive.pylisa.antlr.Python3Parser.Raise_stmtContext;
import it.unive.pylisa.antlr.Python3Parser.Return_stmtContext;
import it.unive.pylisa.antlr.Python3Parser.Yield_argContext;
import it.unive.pylisa.antlr.Python3Parser.Yield_stmtContext;
import it.unive.pylisa.cfg.expression.Break;
import it.unive.pylisa.cfg.expression.Continue;
import it.unive.pylisa.frontend.ParserContext;
import it.unive.pylisa.frontend.ParserSupport;
import java.util.List;
import java.util.Objects;

/**
 * Handles control-flow-terminating statements — return, break, continue, yield,
 * raise, and the {@code flow_stmt} wrapper. Extracted from
 * {@link StatementVisitor} in Chunk 4.
 */
public final class FlowControlVisitor {

	private final ParserContext ctx;
	private final ParserSupport support;

	public FlowControlVisitor(
			ParserContext ctx,
			ParserSupport support) {
		this.ctx = Objects.requireNonNull(ctx);
		this.support = Objects.requireNonNull(support);
	}

	public Statement visitFlow_stmt(
			Flow_stmtContext pctx) {
		if (pctx.return_stmt() != null)
			return visitReturn_stmt(pctx.return_stmt());

		if (pctx.raise_stmt() != null) {
			support.unsound(pctx, "raise treated as no-op");
			return new NoOp(ctx.currentCFG(), support.getLocation(pctx));
		}

		if (pctx.yield_stmt() != null) {
			Yield_argContext yieldArg = pctx.yield_stmt().yield_expr().yield_arg();
			if (yieldArg == null) {
				return new NoOp(ctx.currentCFG(), support.getLocation(pctx));
			}
			List<Expression> l = ctx.expr().extractExpressionsFromYieldArg(yieldArg);
			return new UnresolvedCall(
					ctx.currentCFG(),
					support.getLocation(pctx),
					CallType.STATIC,
					Program.PROGRAM_NAME,
					"yield from",
					LeftToRightEvaluation.INSTANCE,
					l.toArray(new Expression[0]));
		}

		if (pctx.continue_stmt() != null)
			return visitContinue_stmt(pctx.continue_stmt());

		if (pctx.break_stmt() != null)
			return visitBreak_stmt(pctx.break_stmt());

		return support.rejectUnsupported(pctx);
	}

	public Statement visitReturn_stmt(
			Return_stmtContext pctx) {
		if (pctx.testlist() == null)
			return new Ret(ctx.currentCFG(), support.getLocation(pctx));
		if (pctx.testlist().test().size() == 1)
			return new Return(ctx.currentCFG(), support.getLocation(pctx),
					ctx.expr().visitTest(pctx.testlist().test(0)));
		else {
			support.unsound(pctx, "multiple return values treated as first value");
			return new Return(ctx.currentCFG(), support.getLocation(pctx),
					ctx.expr().visitTest(pctx.testlist().test(0)));
		}
	}

	public Statement visitBreak_stmt(
			Break_stmtContext pctx) {
		return new Break(ctx.currentCFG(), support.getLocation(pctx));
	}

	public Statement visitContinue_stmt(
			Continue_stmtContext pctx) {
		return new Continue(ctx.currentCFG(), support.getLocation(pctx));
	}

	public Object visitYield_stmt(
			Yield_stmtContext pctx) {
		// Mirror the yield-handling branch of visitFlow_stmt so that yield
		// reached through the dedicated grammar production (rather than via
		// flow_stmt) is also translated to a control-flow-preserving node.
		// Treating yield as a no-op is unsound for generator semantics, but
		// it keeps the surrounding CFG well-formed — necessary for any
		// project that uses async generators (notably FastAPI's
		// `async def lifespan(app): try: yield finally: ...` pattern).
		support.unsound(pctx, "yield treated as no-op");
		Yield_argContext yieldArg = pctx.yield_expr().yield_arg();
		if (yieldArg == null)
			return new NoOp(ctx.currentCFG(), support.getLocation(pctx));
		List<Expression> l = ctx.expr().extractExpressionsFromYieldArg(yieldArg);
		return new UnresolvedCall(
				ctx.currentCFG(),
				support.getLocation(pctx),
				CallType.STATIC,
				Program.PROGRAM_NAME,
				"yield from",
				LeftToRightEvaluation.INSTANCE,
				l.toArray(new Expression[0]));
	}

	public Object visitRaise_stmt(
			Raise_stmtContext pctx) {
		return support.rejectUnsupported(pctx);
	}
}
