package it.unive.pylisa.libraries.fastapi;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.*;
import it.unive.lisa.program.cfg.statement.literal.TrueLiteral;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.operators.network.HttpServiceEndpointCreation;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.BooleanType;

import java.util.HashMap;

public class Get extends VariadicExpression implements PluggableStatement {
    protected Statement st;
    public Get(
            CFG cfg,
            CodeLocation location,
            Expression[] params) {
        super(cfg, location, "fastapi.get", params, new HashMap<>() {{
            put("path", 1);
        }});
    }

    @Override
    protected int compareSameClassAndParams(
            Statement o) {
        return 0;
    }


    @Override
    public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> fwdVariadicSemantics(InterproceduralAnalysis<A, D> interprocedural, AnalysisState<A> state, SymbolicExpression[] combination, StatementStore<A> expressions) throws SemanticException {
        System.out.println("fastapi.get::fwdVariadicSemantics");

        /*it.unive.lisa.symbolic.value.VariadicExpression expr = new it.unive.lisa.symbolic.value.VariadicExpression.Builder()
                .operator(HttpServiceEndpointCreation.INSTANCE)
                .varargsOperand("basePath", combination[getVarArgsIndex().get("path")])
                .build();*/

        /*it.unive.lisa.symbolic.value.VariadicExpression expr = new it.unive.lisa.symbolic.value.VariadicExpression.Builder()
                .operator(HttpServiceCreation.INSTANCE)
                .varargsOperand("concreteType", new Constant(StringType.INSTANCE, "FastAPI", getLocation()))
                .varargsOperand("basePath", combination[getVarArgsIndex().get("root_path")])
                .staticType(LiSAHttpService.INSTANCE)
                .location(getLocation())
                .build();
        return interprocedural.getAnalysis().assign(state, new NetworkIdentifier(LiSAHttpService.INSTANCE, "FastAPI_HTTPService", false, null, getLocation()), expr, this);*/
        return state;
    }

    public static Get build(
            CFG cfg,
            CodeLocation location,
            Expression[] exprs) {
        return new Get(cfg, location, exprs);
    }

    @Override
    final public void setOriginatingStatement(
            Statement st) {
        this.st = st;
    }
}
