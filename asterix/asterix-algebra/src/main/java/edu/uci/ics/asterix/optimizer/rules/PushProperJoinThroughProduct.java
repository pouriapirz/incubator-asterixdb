package edu.uci.ics.asterix.optimizer.rules;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.Mutable;

import edu.uci.ics.hyracks.algebricks.core.algebra.base.ILogicalExpression;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.ILogicalOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.IOptimizationContext;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalOperatorTag;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalVariable;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.AbstractBinaryJoinOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.AbstractLogicalOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.InnerJoinOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.visitors.VariableUtilities;
import edu.uci.ics.hyracks.algebricks.core.algebra.util.OperatorPropertiesUtil;
import edu.uci.ics.hyracks.algebricks.core.api.exceptions.AlgebricksException;
import edu.uci.ics.hyracks.algebricks.core.rewriter.base.IAlgebraicRewriteRule;

public class PushProperJoinThroughProduct implements IAlgebraicRewriteRule {

    private List<LogicalVariable> usedInCond1AndMaps = new ArrayList<LogicalVariable>();
    private List<LogicalVariable> productLeftBranchVars = new ArrayList<LogicalVariable>();

    @Override
    public boolean rewritePre(Mutable<ILogicalOperator> opRef, IOptimizationContext context) throws AlgebricksException {
        return false;
    }

    @Override
    public boolean rewritePost(Mutable<ILogicalOperator> opRef, IOptimizationContext context) throws AlgebricksException {
        AbstractLogicalOperator op = (AbstractLogicalOperator) opRef.getValue();
        LogicalOperatorTag tag1 = op.getOperatorTag();
        if (tag1 != LogicalOperatorTag.INNERJOIN && tag1 != LogicalOperatorTag.LEFTOUTERJOIN) {
            return false;
        }
        AbstractBinaryJoinOperator join1 = (AbstractBinaryJoinOperator) op;
        ILogicalExpression cond1 = join1.getCondition().getValue();
        // don't try to push a product down
        if (OperatorPropertiesUtil.isAlwaysTrueCond(cond1)) {
            return false;
        }

        Mutable<ILogicalOperator> opRef2 = op.getInputs().get(0);

        AbstractLogicalOperator op2 = (AbstractLogicalOperator) opRef2.getValue();
        while (op2.isMap()) {
            opRef2 = op2.getInputs().get(0);
            op2 = (AbstractLogicalOperator) opRef2.getValue();
        }

        if (op2.getOperatorTag() != LogicalOperatorTag.INNERJOIN) {
            return false;
        }

        InnerJoinOperator product = (InnerJoinOperator) op2;
        if (!OperatorPropertiesUtil.isAlwaysTrueCond(product.getCondition().getValue())) {
            return false;
        }

        usedInCond1AndMaps.clear();
        cond1.getUsedVariables(usedInCond1AndMaps);
        Mutable<ILogicalOperator> opIterRef = op.getInputs().get(0);
        ILogicalOperator opIter = opIterRef.getValue();
        do {
            VariableUtilities.getUsedVariables(opIter, usedInCond1AndMaps);
            opIterRef = opIter.getInputs().get(0);
            opIter = opIterRef.getValue();
        } while (opIter.isMap());

        productLeftBranchVars.clear();
        ILogicalOperator opLeft = op2.getInputs().get(0).getValue();
        VariableUtilities.getLiveVariables(opLeft, productLeftBranchVars);

        if (!OperatorPropertiesUtil.disjoint(usedInCond1AndMaps, productLeftBranchVars)) {
            return false;
        }

        // now push the operators from in between joins, too
        opIterRef = op.getInputs().get(0);
        opIter = opIterRef.getValue();

        Mutable<ILogicalOperator> op3Ref = product.getInputs().get(1);
        ILogicalOperator op3 = op3Ref.getValue();

        opRef2.setValue(op3);
        op3Ref.setValue(join1);
        opRef.setValue(product);
        return true;
    }
}
