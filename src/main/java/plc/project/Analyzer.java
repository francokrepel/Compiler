package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        visit(ast.getCondition());

        for (int i = 0; i < ast.getCases().size(); i++) {
            requireAssignable(ast.getCondition().getType(), ast.getCases().get(i).getValue().get().getType());
        }

        if (ast.getCases().get(ast.getCases().size()).getValue().get().getType() != null) {
            throw new RuntimeException();
        }

        try {
            for (Ast.Statement.Case c : ast.getCases()) {
                scope = new Scope(scope);
                visit(c);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;

        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        //visit(ast.getValue());
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
        // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        //Needs to be worked on with visit(Ast.Function ast)
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if (literal == "NIL" ) {
            ast.setType(Environment.Type.NIL);
        } else if (literal instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (literal instanceof  Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (literal instanceof BigInteger) {
            BigInteger val = (BigInteger) ast.getLiteral();
            if (val.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                    || val.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new RuntimeException("Integer size overflow");
            }
            ast.setType(Environment.Type.INTEGER);
        } else if (literal instanceof BigDecimal) {
            BigDecimal val = (BigDecimal) ast.getLiteral();
            if (val.doubleValue() == Double.NEGATIVE_INFINITY || val.doubleValue() == Double.POSITIVE_INFINITY || val.doubleValue() < Double
                    .MIN_VALUE) { //DOUBLE CHECK THIS ONE
                throw new RuntimeException("Decimal size overflow");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        if (ast.getExpression() instanceof Ast.Expression.Binary) {
            visit(ast.getExpression());
            ast.setType(ast.getExpression().getType());
            return null;
        }
        throw new RuntimeException("Expression is not instanceof Binary Expression");
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        Ast.Expression lhs = ast.getLeft();
        visit(lhs);
        Ast.Expression rhs = ast.getRight();
        visit(rhs);
        switch (ast.getOperator()) {
            case "&&": case "||":
                if (lhs.getType() == Environment.Type.BOOLEAN && rhs.getType() == Environment.Type.BOOLEAN) {
                    ast.setType(Environment.Type.BOOLEAN);
                    return null;
                }
                throw new RuntimeException("Expected two Booleans");
            case "<": case ">": case "==": case "!=":
                if (lhs.getType() == Environment.Type.COMPARABLE && rhs.getType() == Environment.Type.COMPARABLE) {
                    ast.setType(Environment.Type.COMPARABLE);
                    return null;
                }
                throw new RuntimeException("Expected two Comparable types");
            case "+":
                if (lhs.getType() == Environment.Type.STRING || rhs.getType() == Environment.Type.STRING) {
                    ast.setType(Environment.Type.STRING);
                    return null;
                } else if (lhs.getType() == Environment.Type.INTEGER && rhs.getType() == Environment.Type.INTEGER) {
                    ast.setType(Environment.Type.INTEGER);
                    return null;
                } else if (lhs.getType() == Environment.Type.DECIMAL && rhs.getType() == Environment.Type.DECIMAL) {
                    ast.setType(Environment.Type.DECIMAL);
                    return null;
                }
                throw new RuntimeException("Expected two Integers, two Decimals, or at least one String");
            case "-": case "*": case "/":
                if (lhs.getType() == Environment.Type.INTEGER && rhs.getType() == Environment.Type.INTEGER) {
                    ast.setType(Environment.Type.INTEGER);
                    return null;
                } else if (lhs.getType() == Environment.Type.DECIMAL && rhs.getType() == Environment.Type.DECIMAL) {
                    ast.setType(Environment.Type.DECIMAL);
                    return null;
                }
                throw new RuntimeException("Expected two Integers or two Decimals");
            case "^":
                if (lhs.getType() == Environment.Type.INTEGER && rhs.getType() == Environment.Type.INTEGER) {
                    ast.setType(Environment.Type.INTEGER);
                    return null;
                }
                throw new RuntimeException("Expected two Integers");
        }
        throw new RuntimeException("uh oh spaghettio");
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        ast.setVariable(scope.lookupVariable(ast.getName()));
        try {
            if (ast.getOffset().get().getType() != Environment.Type.INTEGER) {
                throw new RuntimeException();
            }
        } catch (NoSuchElementException e) {}
        return null;
        // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function f = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        ast.setFunction(f);

        for (int i = 0; i < f.getParameterTypes().size(); i++) {
            requireAssignable(ast.getArguments().get(i).getType(), f.getParameterTypes().get(i));
        }

        return null;
        // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        for (Ast.Expression e : ast.getValues()) {
            requireAssignable(ast.getType(), e.getType());
        }
        //throw new UnsupportedOperationException();  // TODO
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target == Environment.Type.COMPARABLE) {
            if (type == Environment.Type.BOOLEAN) {
                throw new RuntimeException();
            }
        } else if (target != type) {
            if (target != Environment.Type.ANY)
                throw new RuntimeException();
        }
    }

}
