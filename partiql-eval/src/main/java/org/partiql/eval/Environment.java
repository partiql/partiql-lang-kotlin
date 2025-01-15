package org.partiql.eval;

import org.partiql.spi.value.Datum;

/**
 * This class holds the evaluation environment.
 * <br>
 * Developer Note: Attempts have been made at an interpreter stack, but has not worked well with nested lazy values.
 * For example, an expression may return an inner lazy value (think subqueries), but the stack state may have changed
 * BEFORE the inner lazy value is accessed. Here is my best attempt at illustrating this,
 * <code>
 * 0:       +-PUSH(row)
 * 1:   +--|----bag.eval()    // lazy iterator (push/pop and eval variables)
 * 2:   |   +-POP
 * 3:   |
 * 4:   +-- .next()           // woah! we called bag.next() from line:1, but we popped on line:2 so line:1 is invalid!
 * </code>
 * <br>
 * The most basic solution we have is to pass a new environment into each nested scope.
 */
public class Environment {

    private final Row[] stack;

    /**
     * Default constructor with empty stack.
     */
    public Environment() {
        this.stack = new Row[]{};
    }

    /**
     * Private constructor with given stack.
     * @param stack
     */
    private Environment(Row[] stack) {
        this.stack = stack;
    }

    /**
     * Push a new row onto the stack.
     */
    public Environment push(Row row) {
        int n = stack.length;
        Row[] next = new Row[n + 1];
        next[0] = row;
        if (n > 0) {
            System.arraycopy(stack, 0, next, 1, n);
        }
        return new Environment(next);
    }

    /**
     * Returns the variable at the specified depth and offset.
     *
     * @param depth     Scope depth.
     * @param offset    Variable offset.
     * @return  Datum.
     */
    public Datum get(int depth, int offset) {
        try {
            return stack[depth].values[offset];
        } catch (IndexOutOfBoundsException ex) {
            throw new RuntimeException("Invalid variable reference [$depth:$offset]\n$this");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[stack]--------------\n");
        for (int i = 0; i < stack.length; i++) {
            sb.append("$i: $row");
            sb.append("---------------------");
        }
        if (stack.length == 0) {
            sb.append("empty\n");
            sb.append("---------------------\n");
        }
        return sb.toString();
    }
}
