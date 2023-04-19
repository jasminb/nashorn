package org.openjdk.nashorn.internal.ir.visitor;

import org.openjdk.nashorn.api.scripting.ScriptUtils;
import org.openjdk.nashorn.internal.ir.*;

import java.util.Collections;

/**
 * Visits JS AST and injects configured `trap function` into loops and function calls.
 *
 * <p/>
 */
public class TrapFunctionVisitor extends SimpleNodeVisitor {
    private final String trapName;

    public TrapFunctionVisitor(String trapName) {
        this.trapName = trapName;
    }

    @Override
    public Node leaveWhileNode(WhileNode node) {
        if (hasTrap(node.getBody())) {
            return node;
        }
        return node.setBody(
                this.getLexicalContext(),
                injectFun(node.getToken(), node.getFinish(), node.getLineNumber(), node.getBody())
        );
    }

    @Override
    public Node leaveForNode(ForNode node) {
        if (hasTrap(node.getBody())) {
            return node;
        }
        return node.setBody(
                this.getLexicalContext(),
                injectFun(node.getToken(), node.getFinish(), node.getLineNumber(), node.getBody())
        );
    }

    @Override
    public Node leaveFunctionNode(FunctionNode node) {
        if (hasTrap(node.getBody())) {
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

    private boolean hasTrap(Block block) {
        return block.getStatements().stream()
                .filter(s -> !ScriptUtils.containsTrapPragma(s.toString()))
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
