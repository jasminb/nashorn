package org.openjdk.nashorn.internal.ir.visitor;

import org.openjdk.nashorn.internal.ir.*;

import java.util.Collections;
import java.util.regex.Pattern;

/**
 * Injects custom code block (function) into several different parsed JS AST nodes.
 *
 * <p/>
 */
public class FunctionTrapVisitor extends SimpleNodeVisitor {

    // Captures pragma such as 'trap_function my_function';
    public static final Pattern TRAP_FUNCTION_PRAGMA_PATTERN = Pattern.compile(
            "(\"|')trap_function\\s([A-Za-z_\\-0-9]+)(\"|');"
    );
    public static String TRAP_PRAGMA = "trap_function";
    private final String trapName;

    public FunctionTrapVisitor(String trapName) {
        this.trapName = trapName;
    }

    @Override
    public Node leaveWhileNode(WhileNode node) {
        if (hasInterrupt(node.getBody())) {
            return node;
        }
        return node.setBody(
                this.getLexicalContext(),
                injectFun(node.getToken(), node.getFinish(), node.getLineNumber(), node.getBody())
        );
    }

    @Override
    public Node leaveForNode(ForNode node) {
        if (hasInterrupt(node.getBody())) {
            return node;
        }
        return node.setBody(
                this.getLexicalContext(),
                injectFun(node.getToken(), node.getFinish(), node.getLineNumber(), node.getBody())
        );
    }

    @Override
    public Node leaveFunctionNode(FunctionNode node) {
        if (hasInterrupt(node.getBody())) {
            return node;
        }

        if (node.getBody().getStatements().isEmpty()) {
            return node;
        }

        if (node.getName().equals(trapName)) {
            return node;
        }

        return node.setBody(
                this.getLexicalContext(),
                injectFun(node.getToken(), node.getFinish(), node.getLineNumber(), node.getBody())
        );
    }

    private boolean hasInterrupt(Block block) {
        return block.getStatements().stream()
                .filter(s -> !s.toString().contains(TRAP_PRAGMA))
                .anyMatch(s -> s.toString().contains(trapName));
    }

    private Block injectFun(long token, int finish, int lineNumber, Block innerBody) {
        IdentNode identNode = new IdentNode(token, finish, trapName);

        return new Block(
                token,
                finish,
                new ExpressionStatement(
                        lineNumber,
                        token,
                        finish,
                        new CallNode(lineNumber, token, finish, identNode, Collections.emptyList(), false)
                ),
                new BlockStatement(innerBody)
        );
    }
}
