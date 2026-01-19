package it.unive.pylisa.cfg.statement;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.BinaryExpression;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;

import java.util.List;

public class FunctionApply extends BinaryExpression {

    public FunctionApply(CFG cfg, CodeLocation location, Expression left, Expression right) {
        super(cfg, location, "function-apply", left, right);
    }
    @Override
    public String toString() {
        return getLeft().toString() + "(" + getRight().toString() + ")";
    }
    @Override
    public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdBinarySemantics(InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression left, SymbolicExpression right, StatementStore<A> expressions) throws SemanticException {
        if (left instanceof Constant) {
            Constant constant = (Constant) left;
            if (left.getStaticType() instanceof FunctionLiteral l) {
                CFG leftCFG = l.getValue();
                CFGCall call = new CFGCall(this.getCFG(), getLocation(), Call.CallType.UNKNOWN, (String) null, leftCFG.getDescriptor().getName(), List.of(leftCFG), this.getRight());

            }
        }
        return state;
    }

    @Override
    protected int compareSameClassAndParams(Statement o) {
        return 0;
    }
}
