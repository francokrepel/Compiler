package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;
    public Environment.Type returnType;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        Ast.Function main = null;
        for (Ast.Global g : ast.getGlobals()) {
            visit(g);
        }
        for (Ast.Function f : ast.getFunctions()) {
            if (f.getName().equals("main")) {
                main = f;
            }
        }
        if (main == null || main.getParameters().size() != 0 || !main.getReturnTypeName().equals("Integer")) {
            throw new RuntimeException();
        }
        for (Ast.Function f : ast.getFunctions()) {
            visit(f);
        }
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
        visit(ast.getValue().get());
        scope.defineVariable(ast.getName(), ast.getName(), Environment.getType(ast.getTypeName()), ast.getMutable(), Environment.NIL);
        return null;


        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        List<Environment.Type> paramTypes = new ArrayList<>();
        for (String s : ast.getParameterTypeNames()) {
            paramTypes.add(Environment.getType(s));
        }

        ast.setFunction(scope.defineFunction(ast.getName(), ast.getName(), paramTypes, Environment.getType(ast.getReturnTypeName().get()), args -> Environment.NIL));

        try {
            scope = new Scope(scope);
//            for (String s : ast.getParameters()) {
//                scope.defineVariable(s, true, ast.get);
//            }
            int i;
            for (i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
            }
            returnType = Environment.getType(ast.getReturnTypeName().get());
//            i++;
//            returnType = ast.getStatements().get(i).

        } finally {
            scope = scope.getParent();
        }

        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if ( !(ast.getExpression() instanceof Ast.Expression.Function))  {
            throw new RuntimeException("Expression is not of type Function");  // TODO
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        if (!ast.getTypeName().isPresent() && !ast.getValue().isPresent()) {
            throw new RuntimeException("Declaration statement must have type or value:");
        }
        Environment.Type type = null;
        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());

            if (type == null) {
                type = ast.getValue().get().getType();
            }

            requireAssignable(type, ast.getValue().get().getType());
        }
//        scope.defineVariable();
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, true, Environment.NIL));
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("receiver is not an access expression ");
        }
        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
//        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException();
        }
        try {
            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getThenStatements()) {
                visit(stmt);
            }

            scope = new Scope(scope);
            for (Ast.Statement stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;

        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        try {
            scope = new Scope(scope);
            visit(ast.getCondition());
            for (int i = 0; i < ast.getCases().size() - 1; i++) { // - 1 as we have a default last
                visit(ast.getCases().get(i).getValue().get());
                requireAssignable(ast.getCondition().getType(), ast.getCases().get(i).getValue().get().getType());
            }
            Ast.Statement.Case defaultCase = ast.getCases().get(ast.getCases().size()-1);
            visit(defaultCase);
            if (defaultCase.getValue().isPresent() ) {
                throw new RuntimeException("Exception at Default Case");
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        try {
            scope = new Scope(scope);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
//        throw new UnsupportedOperationException();  // TODO
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
//        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        requireAssignable(returnType, ast.getValue().getType());
        return null;
        //throw new UnsupportedOperationException();  // TODO
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
        Ast.Expression rhs = ast.getRight();
        visit(lhs);
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
                } else {
                    if (IntegersOrDecimals(ast, lhs, rhs)) return null;
                }
                throw new RuntimeException("Expected two Integers, two Decimals, or at least one String");
            case "-": case "*": case "/":
                if (IntegersOrDecimals(ast, lhs, rhs)) return null;
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

    //helper method for binary
    private boolean IntegersOrDecimals(Ast.Expression.Binary ast, Ast.Expression lhs, Ast.Expression rhs) {
        if (lhs.getType() == Environment.Type.INTEGER && rhs.getType() == Environment.Type.INTEGER) {
            ast.setType(Environment.Type.INTEGER);
            return true;
        } else if (lhs.getType() == Environment.Type.DECIMAL && rhs.getType() == Environment.Type.DECIMAL) {
            ast.setType(Environment.Type.DECIMAL);
            return true;
        }
        return false;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        ast.setVariable(scope.lookupVariable(ast.getName()));
        if (ast.getOffset().isPresent()) {
            if (ast.getOffset().get().getType() != Environment.Type.INTEGER) {
                throw new RuntimeException("offset is not of Integer type");
            }
        }
        return null;
        // TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function f = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
//            System.out.println(ast.getArguments().get(i).getType());
//            System.out.println(f.getParameterTypes().get(i));
            requireAssignable(f.getParameterTypes().get(i), ast.getArguments().get(i).getType());
        }
        ast.setFunction(f);
        ast.setFunction(ast.getFunction());
        return null;
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
