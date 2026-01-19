package it.unive.pylisa.cfg.statement;

import it.unive.lisa.analysis.*;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.CompilationUnit;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;

import java.util.ArrayList;
import java.util.List;

public class DecoratedFunctionDefinition extends Statement {
    List<Statement> decorators = new ArrayList<>();
    FunctionLiteral function;
    String funtionName;
    /**
     * Builds a statement happening at the given source location.
     *
     * @param cfg      the cfg that this statement belongs to
     * @param location the location where this statement is defined within the
     *                 program
     */
    protected DecoratedFunctionDefinition(CFG cfg, CodeLocation location) {
        super(cfg, location);
    }

    @Override
    public String toString() {
        return "<dec-func> " + function + "{ " + decorators + " }";
    }

    @Override
    public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(AnalysisState<A> entryState, InterproceduralAnalysis<A, D> interprocedural, StatementStore<A> expressions) throws SemanticException {
        AnalysisState<A> result = entryState.bottom();
        for (Statement s : decorators) {
            result = result.lub(s.forwardSemantics(entryState, interprocedural, expressions));
            ExpressionSet exprSet = result.getExecution().getComputedExpressions();
            for (SymbolicExpression e : exprSet) {
                // ???
            }
        }

        CFG decoratedFunction = function.getValue();
        if (decoratedFunction.getUnit() instanceof CompilationUnit cu) {
            CodeMemberDescriptor descriptor =  new CodeMemberDescriptor(decoratedFunction.getDescriptor().getLocation(), decoratedFunction.getUnit(), decoratedFunction.getDescriptor().isInstance(), funtionName, decoratedFunction.getDescriptor().getFormals());
            // add the decorated function.
            /*if (descriptor.isInstance()) {
                cu.addInstanceCodeMember(new NativeCFG(descriptor, decoratedFunction)); // decoratedFunction should be NativeCFG
            } else {
                cu.addCodeMember(new NativeCFG(descriptor, decoratedFunction));
            }*/
        }
        return result;
    }

    @Override
    protected int compareSameClass(Statement o) {
        return 0;
    }

    @Override
    public <V> boolean accept(GraphVisitor<CFG, Statement, Edge, V> visitor, V tool) {
        return visitor.visit(tool, getCFG(), this);
    }
}
