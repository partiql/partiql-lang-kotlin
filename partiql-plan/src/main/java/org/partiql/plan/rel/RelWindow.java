package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Collation;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.WindowFunctionNode;
import org.partiql.plan.rex.Rex;
import org.partiql.spi.types.PType;
import org.partiql.spi.types.PTypeField;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This represents the WINDOW operator.
 * </p>
 * <p>
 * The output of the window operator contains, in the following order:
 * <ul>
 *     <li>the input's bindings</li>
 *     <li>the window functions' outputs</li>
 * </ul>
 * </p>
 */
public abstract class RelWindow extends RelBase {

    /**
     * Creates a new {@link RelWindow} instance.
     *
     * @param input the input
     * @param windowFunctions the window functions to apply
     * @param collations the collations to sort by
     * @param partitions the partitions to partition by
     * @return new {@link RelWindow} instance
     */
    @NotNull
    public static RelWindow create(
            @NotNull Rel input,
            @NotNull List<String> windowFunctionBindings,
            @NotNull List<WindowFunctionNode> windowFunctions,
            @NotNull List<Collation> collations,
            @NotNull List<Rex> partitions
    ) {
        return new Impl(input, windowFunctionBindings, windowFunctions, collations, partitions);
    }

    /**
     * Gets the input (operand 0).
     * @return the input (operand 0).
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * Returns the collations to sort by.
     * @return the collations to sort by
     */
    @NotNull
    public abstract List<Collation> getCollations();

    /**
     * Returns the window functions to apply.
     * @return the window functions to apply
     */
    @NotNull
    public abstract List<WindowFunctionNode> getWindowFunctions();

    /**
     * Returns the partitions to partition by.
     * @return the partitions to partition by
     */
    @NotNull
    public abstract List<Rex> getPartitions();

    @NotNull
    @Override
    protected final List<Operand> operands() {
        List<Operand> list = new ArrayList<Operand>();
        list.add(Operand.single(getInput()));
        return list;
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitWindow(this, ctx);
    }

    private static class Impl extends RelWindow {

        private final Rel input;
        private final List<Collation> collations;
        private final List<String> windowFunctionBindings;
        private final List<WindowFunctionNode> windowFunctions;
        private final List<Rex> partitions;

        private Impl(Rel input, List<String> windowFunctionBindings, List<WindowFunctionNode> windowFunctions, List<Collation> collations, List<Rex> partitions) {
            this.input = input;
            this.collations = collations;
            this.windowFunctionBindings = windowFunctionBindings;
            this.windowFunctions = windowFunctions;
            this.partitions = partitions;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public List<Collation> getCollations() {
            return collations;
        }

        @NotNull
        @Override
        public List<WindowFunctionNode> getWindowFunctions() {
            return windowFunctions;
        }

        @NotNull
        @Override
        public List<Rex> getPartitions() {
            return partitions;
        }

        @NotNull
        @Override
        protected final RelType type() {
            // Create Output Fields
            PTypeField[] inputFields = getInput().getType().getFields();
            PTypeField[] outputFields = new PTypeField[inputFields.length + getWindowFunctions().size()];

            // Copy input fields and window functions
            System.arraycopy(inputFields, 0, outputFields, 0, inputFields.length);
            int start = inputFields.length;
            for (int i = 0; i < getWindowFunctions().size(); i++) {
                // throw new IllegalStateException("Not yet implemented");
                PType outputType = getWindowFunctions().get(i).getSignature().getReturnType();
                outputFields[start + i] = PTypeField.of(windowFunctionBindings.get(i), outputType);
            }
            return RelType.of(outputFields);
        }
    }
}
