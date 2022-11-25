package plc.project;

import java.io.PrintWriter;
import java.util.Optional;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        writer.write("public class Main {");
        newline(0);
        for (Ast.Global g : ast.getGlobals()) {
            newline(1);
            visit(g);
        }

        //newline(1);
        newline(1);
        writer.write("public static void main(String[] args) {");
        newline(2);
        writer.write("System.exit(new Main().main());");
        newline(1);
        writer.write("}");
        newline(0);

        for (Ast.Function f : ast.getFunctions()) {
            newline(1);
            visit(f);
        }
        newline(0);
        newline(0);
        writer.write("}");
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        if (ast.getVariable().getJvmName().equals("list")) { //this might be wrong
            writer.write(ast.getVariable().getType().getJvmName() + "[] " + ast.getVariable().getName() + " = ");
            visit(ast.getValue().get());
            writer.write(";");
        } else if (ast.getMutable()) {
            writer.write(ast.getName() + " " + ast.getVariable().getName());

            if (ast.getValue().isPresent()) {
                writer.write(" = ");
                visit(ast.getValue().get());
                writer.write(";");
            }
        } else {
            writer.write("final " + ast.getName() + " " + ast.getVariable().getName());

            if (ast.getValue().isPresent()) {
                writer.write(" = ");
                visit(ast.getValue().get());
                writer.write(";");
            }
        }
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        writer.write(ast.getFunction().getReturnType().getJvmName() + " " + ast.getFunction().getName() + "(");

        if (ast.getParameters().size() == 1) {
            writer.write(ast.getParameterTypeNames().get(0) + " " + ast.getParameters().get(0)); //type name printing incorrectly
        } else if (ast.getParameters().size() > 1) {
            for (int i = 0; i < ast.getParameters().size()-1; i++) {
                writer.write(ast.getParameterTypeNames().get(0) + " " + ast.getParameters().get(0)); //type name printing incorrectly
                writer.write(", ");
            }
            writer.write(ast.getParameterTypeNames().get(ast.getParameters().size()-1) + " " + ast.getParameters().get(0));
        }
        writer.write(") {");

        if (ast.getStatements().isEmpty()) {
            writer.write("}");
        } else {
            for (Ast.Statement s : ast.getStatements()) {
                newline(2);
                visit(s);
            }
            newline(1);
            writer.write("}");
        }
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        writer.write(ast.getVariable().getType().getJvmName() + " " + ast.getName());
        if (ast.getValue().isPresent()) {
            writer.write(" = ");
            visit(ast.getValue().get());
        }
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        writer.write(" = ");
        visit(ast.getValue());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        writer.write("if (");
        visit(ast.getCondition());
        writer.write(") {");
        for (Ast.Statement s : ast.getThenStatements()) {
            newline(1);
            visit(s);
        }
        newline(0);
        writer.write("}");

        if (!ast.getElseStatements().isEmpty()) {
            writer.write(" else {");
            for (Ast.Statement s : ast.getElseStatements()) {
                newline(1);
                visit(s);
            }
            newline(0);
            writer.write("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        writer.write("switch (");
        visit(ast.getCondition());
        writer.write(") {");

        for (Ast.Statement.Case c : ast.getCases()) {
            newline(1);
            visit(c);
        }

        newline(0);
        writer.write("}");
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        if (ast.getValue().equals(Optional.empty())) {
            writer.write("default:");
            for (Ast.Statement s : ast.getStatements()) {
                newline(2);
                visit(s);
            }
        } else {
            writer.write("case ");
            visit(ast.getValue().get());
            writer.write(":");
            for (Ast.Statement s : ast.getStatements()) {
                newline(2);
                visit(s);
            }
        }
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        writer.write("while (");
        visit(ast.getCondition());
        writer.write(") {");

        if (ast.getStatements().isEmpty()) {
            writer.write("}");
        } else {
            for (Ast.Statement s : ast.getStatements()) {
                newline(1);
                visit(s);
            }
            newline(0);
            writer.write("}");
        }
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        writer.write("return ");
        visit(ast.getValue());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if (ast.getType().equals(Environment.Type.CHARACTER)) {
            writer.write("\'");
            writer.write(ast.getLiteral().toString());
            writer.write("\'");
        } else if (ast.getType().equals(Environment.Type.STRING)) {
            writer.write("\"");
            writer.write(ast.getLiteral().toString());
            writer.write("\"");
        } else {
            writer.write(ast.getLiteral().toString());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        writer.write("(");
        visit(ast.getExpression());
        writer.write(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if (ast.getOperator().equals("^")) {
            writer.write("Math.pow(");
            visit(ast.getLeft());
            writer.write(", ");
            visit(ast.getRight());
            writer.write(")");
        } else {
            visit(ast.getLeft());
            writer.write(" ");
            writer.write(ast.getOperator() + " ");
            visit(ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        if (ast.getOffset().isPresent()) {
            writer.write(ast.getVariable().getJvmName() + "[");
            print(ast.getOffset().get()); //this might still be wrong
            writer.write("]");
        } else {
            writer.write(ast.getVariable().getJvmName());
        }
        return null;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        writer.write(ast.getFunction().getJvmName());
        writer.write("(");
        if (ast.getArguments().size() == 1) {
            visit(ast.getArguments().get(0));
            writer.write(")");
        } else if (ast.getArguments().size() == 0) {
            writer.write(")");
        } else {
            for (int i = 0; i < ast.getArguments().size()-1; i++) {
                visit(ast.getArguments().get(i));
                writer.write(", ");
            }
            visit(ast.getArguments().get(ast.getArguments().size()-1));
            writer.write(")");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        writer.write("{");
        if (ast.getValues().size() == 1) {
            visit(ast.getValues().get(0));
            writer.write("}");
        } else {
            for (int i = 0; i < ast.getValues().size()-1; i++) {
                visit(ast.getValues().get(i));
                writer.write(", ");
            }
            visit(ast.getValues().get(ast.getValues().size()-1));
            writer.write("}");
        }
        return null;
    }

}
