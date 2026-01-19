package it.unive.pylisa.cfg.statement;

import it.unive.lisa.analysis.*;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.literal.Literal;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.pylisa.cfg.PyCFG;
import it.unive.pylisa.cfg.type.PyFunctionType;

public class FunctionLiteral extends Literal<CFG> {
	public FunctionLiteral(
			CFG cfg,
			CodeLocation location,
			PyCFG method) {
		super(cfg, location, method, PyFunctionType.INSTANCE);
	}

	@Override
	public String toString() {
		return "<function> " + getValue().toString();
	}


	@Override
	protected int compareSameClass(
			Statement o) {
		FunctionLiteral other = (FunctionLiteral) o;
		if (getValue().equals(other.getValue())) {
			return 0;
		}
		return 1;
	}

	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FunctionLiteral other = (FunctionLiteral) obj;
		return getValue().equals(other.getValue());
	}

}
