package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        scope.defineFunction("logarithm", 1, args -> {
            if (!(args.get(0).getValue() instanceof BigDecimal)) {
                throw new RuntimeException("Expected a BigDecimal, received " + args.get(0).getValue().getClass().getName() + ".");
            }

            BigDecimal bd1 = (BigDecimal) args.get(0).getValue();

            BigDecimal bd2 = requireType(BigDecimal.class, Environment.create(args.get(0).getValue()));

            BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));

            return Environment.create(result);
        });

        scope.defineFunction("converter", 2, args -> {
            String number = new String();
            int i, n = 0;
            ArrayList<BigInteger> quotients = new ArrayList<BigInteger>();
            ArrayList<BigInteger> remainders = new ArrayList<BigInteger>();

            BigInteger base10 = requireType( //start w this
                    BigInteger.class,
                    Environment.create(args.get(0).getValue() )
            );
            BigInteger base = requireType( //want to shift into this base value
                    BigInteger.class,
                    Environment.create(args.get(1).getValue() )
            );

            quotients.add(base10);

            do {
                quotients.add(quotients.get(n).divide(base));
                remainders.add(quotients.get(n).subtract(quotients.get(n+1).multiply(base)));
                n++;
            } while(quotients.get(n).compareTo(BigInteger.ZERO) > 0);

            for (i = 0; i < remainders.size(); i++) {
                number = remainders.get(i).toString() + number;
            }
            return Environment.create(number);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        List<Ast.Global> g = ast.getGlobals();
        List<Ast.Function> f = ast.getFunctions();

        for (Ast.Global globe : g) {
            scope.defineVariable(globe.getName(), globe.getMutable(), visit(globe.getValue().get()));
        }

        for (Ast.Function func : f) {
            //scope.defineFunction(func.getName(), func.getParameters().size(), );
        }
        return Environment.NIL;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {

        Optional optional = ast.getValue();
        Boolean present = optional.isPresent();

        if (present) {
            Ast.Expression expr = (Ast.Expression) optional.get();

            scope.defineVariable(ast.getName(), true, visit(expr));
        } else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {

        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {

        Optional optional = ast.getValue();
        Boolean present = optional.isPresent();

        if (present) {
            Ast.Expression expr = (Ast.Expression) optional.get();

            scope.defineVariable(ast.getName(), true, visit(expr));
        } else {
            scope.defineVariable(ast.getName(), true, Environment.NIL);
        }

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
//        System.out.println("hi");
//        requireType(Ast.Expression.Access.class, visit(ast.getReceiver()));
//        System.out.println("hiii bye");
//        String recieverName = ast.getReceiver().toString();
//        Environment.PlcObject value = visit(ast.getValue());
//        scope.lookupVariable(ast.getReceiver().toString()).setValue(visit(ast.getValue()));
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getThenStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
                ast.getElseStatements().forEach(this::visit);
            }
        }

        return Environment.NIL;

        //      throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        //SO DEFAULT WILL ALSO BE IN GETCASES THEREFORE U CANT JUST CHECK IF
        //GET CASES IS EMPTY OR NOT,
        try {
            scope = new Scope(scope);
            for (int i = 0; i < ast.getCases().size(); i++) {
                if (!ast.getCases().get(i).getValue().isPresent()) {
                    continue;
                }
                Object condition = requireType(Object.class, visit(ast.getCondition()));
                Object caseValue = requireType(Object.class, visit(ast.getCases().get(i).getValue().get()));
                if (condition.equals(caseValue)) {
                    visit(ast.getCases().get(i)); //if condition == case value, go and enter that case
                }
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
//        throw new UnsupportedOperationException(); //TODO
        //each switch has a condition and a list of cases,
        //each case has a value and a list of statements to do

        //therefore, if the list of cases is not empty,
        //for each case, check if condition is equal to the value
        //if it is, you want to execute the statements
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        try {
            scope = new Scope(scope);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                visit(ast.getStatements().get(i));
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
//        throw new UnsupportedOperationException(); //TODO
    }

    // remeber Boolean by itself is not a class its Declaration for a type
    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
//                for (Ast.Statement stmt : ast.getStatements()) { //iterate through each statement
//                    visit(stmt);
//                }
                ast.getStatements().forEach(this::visit); //remeber each ast.Statement is subclass of AST so by itself it has visit
            } finally { //executes after try and after any exceptions
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
//        throw new UnsupportedOperationException(); //TODO (in lecture)
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Environment.PlcObject val = visit(ast.getValue());
        throw new Return(val);
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return  Environment.create(ast.getLiteral());
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        //later add the second && as another if statement, so it makes it easier to throw an error accruately?

        // && / ||
        if (visit(ast.getLeft()).getValue() instanceof Boolean) {
            switch (ast.getOperator()) {
                case "&&":
                    if (!requireType(Boolean.class, visit(ast.getLeft()))) { //if LHS is false, SHORT CIRCUIT FALSE IF LHS FALSE
                        return Environment.create(false);
                    } else if (visit(ast.getRight()).getValue() instanceof Boolean
                            && !requireType(Boolean.class, visit(ast.getRight()))) {
                        return Environment.create(false);
                    }
                    return Environment.create(true);
                case "||":
                    if (requireType(Boolean.class, visit(ast.getLeft()))) { //if LHS is true, SHORT CIRCUIT TRUE IF LHS TRUE
                        return Environment.create(true);
                    } else if (requireType(Boolean.class, visit(ast.getRight()))
                            && visit(ast.getRight()).getValue() instanceof Boolean) {
                        return Environment.create(true);
                    }
                    return Environment.create(false);
            }
        }
        //  < / > / == / !=
        if (visit(ast.getLeft()).getValue() instanceof Comparable) {
            if (visit(ast.getRight()).getValue() instanceof Comparable) {
                switch (ast.getOperator()) {
                    case "<":
                        int lt = requireType(Comparable.class ,visit(ast.getLeft())).compareTo(requireType(Comparable.class ,visit(ast.getRight())));
                        if (lt < 0) {
                            return Environment.create(true);
                        }
                        return Environment.create(false);
                    case ">":
                        int gt = requireType(Comparable.class ,visit(ast.getLeft())).compareTo(requireType(Comparable.class ,visit(ast.getRight())));
                        if (gt > 0) {
                            return Environment.create(true);
                        }
                        return Environment.create(false);
                    case "==":
                        int eq = requireType(Comparable.class ,visit(ast.getLeft())).compareTo(requireType(Comparable.class ,visit(ast.getRight())));
                        if (eq == 0) {
                            return Environment.create(true);
                        }
                        return Environment.create(false);
                    case "!=":
                        int ne = requireType(Comparable.class ,visit(ast.getLeft())).compareTo(requireType(Comparable.class ,visit(ast.getRight())));
                        if (ne != 0) {
                            return Environment.create(true);
                        }
                        return Environment.create(false);
                }
            }
        }
        //Integers
        if (visit(ast.getLeft()).getValue() instanceof BigInteger && visit(ast.getRight()).getValue() instanceof BigInteger) {  //SEPERATE INTO TWO SO U CAN THROW ERROR?
            switch (ast.getOperator()) {
                case "+":
                    return Environment.create(requireType(BigInteger.class ,visit(ast.getLeft())).add(requireType(BigInteger.class, visit(ast.getRight()))));
                case "-":
                    return Environment.create(requireType(BigInteger.class ,visit(ast.getLeft())).subtract(requireType(BigInteger.class, visit(ast.getRight()))));
                case "*":
                    return Environment.create(requireType(BigInteger.class ,visit(ast.getLeft())).multiply(requireType(BigInteger.class, visit(ast.getRight()))));
                case "/": //returns ceiling
                    return Environment.create(requireType(BigInteger.class ,visit(ast.getLeft())).divide(requireType(BigInteger.class, visit(ast.getRight()))));
                case "^":
                    return Environment.create(requireType(BigInteger.class ,visit(ast.getLeft())).modPow(requireType(BigInteger.class,visit(ast.getRight())), new BigInteger("9223372036854775807")));
                case "<":
            }
        }
        //Decimals
        if (visit(ast.getLeft()).getValue() instanceof BigDecimal && visit(ast.getRight()).getValue() instanceof BigDecimal) {  //SEPERATE INTO TWO SO U CAN THROW ERROR?
            switch (ast.getOperator()) {
                case "+":
                    return Environment.create(requireType(BigDecimal.class ,visit(ast.getLeft())).add(requireType(BigDecimal.class, visit(ast.getRight()))));
                case "-":
                    return Environment.create(requireType(BigDecimal.class ,visit(ast.getLeft())).subtract(requireType(BigDecimal.class, visit(ast.getRight()))));
                case "*":
                    return Environment.create(requireType(BigDecimal.class ,visit(ast.getLeft())).multiply(requireType(BigDecimal.class, visit(ast.getRight()))));
                case "/":
                    return Environment.create(requireType(BigDecimal.class ,visit(ast.getLeft())).divide(requireType(BigDecimal.class, visit(ast.getRight())), RoundingMode.HALF_EVEN));
                //it seems that there is no way to do power of two big Decimals
//                case "^":
//                    return Environment.create(requireType(BigDecimal.class ,visit(ast.getLeft())).modPow(requireType(BigDecimal.class,visit(ast.getRight()))));
            }
        }
        // if not then throw exception as you may have one integer and a different type, or used an unknown operator?
        //Concatenation
        if (visit(ast.getLeft()).getValue() instanceof String && visit(ast.getRight()).getValue() instanceof String) {
            switch (ast.getOperator()) {
                case "+":
                    return Environment.create(requireType(String.class ,visit(ast.getLeft())).concat(requireType(String.class, visit(ast.getRight()))));
            }

        }
        throw new UnsupportedOperationException(); //TODO
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        Optional<Ast.Expression> offset = ast.getOffset();
        if (offset.isPresent()) { //list variable
            BigInteger _offset = requireType(BigInteger.class, visit(offset.get()));
            return Environment.create(requireType(List.class, scope.lookupVariable(ast.getName()).getValue())
                    .get(_offset.intValue()));
        } else { //normal variable
            return Environment.create(scope.lookupVariable(ast.getName()).getValue().getValue());
        }
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        List<Environment.PlcObject> arguments = new ArrayList<Environment.PlcObject>();
        for (int i = 0; i < ast.getArguments().size(); i++) {
            arguments.add(visit(ast.getArguments().get(i)));
        }
        return Environment.create(scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(arguments).getValue());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        List<Object> expected = new ArrayList<Object>();
        for (int i = 0; i < ast.getValues().size(); i++) {
            expected.add(requireType(BigInteger.class, visit(ast.getValues().get(i))));
        }
        return Environment.create(expected);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}