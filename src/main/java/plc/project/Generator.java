package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
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
        print("public class Main {");
        newline(indent);
        indent++;
        newline(indent);
        if (!ast.getGlobals().isEmpty()) {
            for (Ast.Global g: ast.getGlobals()) {
                visit(g);
            }
        }
        print("public static void main(String[] args) {");
        indent++;
        newline(indent);
        print("System.exit(new Main().main());");
        indent--;
        newline(indent);
        print("}");
        newline(0);
        newline(indent);
        for (Ast.Function f:ast.getFunctions()) {
            visit(f);
        }
        newline(0);
        newline(0);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        if(ast.getMutable()){
            //mutable
            if(ast.getVariable().getJvmName().equals("list")){
                //TODO: list- check OH
                print(ast.getVariable().getType().getJvmName());
                print("[]");
                print(" ");
                print(ast.getName());
                print(" ");
                print("=");
                print(" ");
                print("{");
                if(ast.getValue().isPresent()){
                    Ast.Expression.PlcList temp = (Ast.Expression.PlcList)ast.getValue().get();
                    visit(temp);
                }
                print("}");
                print(";");

            }else{
                print(ast.getVariable().getType().getJvmName());
                if(ast.getValue().isPresent()){
                    print(" ");
                    print("=");
                    print(" ");
                    print(ast.getValue());
                    print(";");
                }
            }
        }else{
            //immutable
            print("final ");
            print(ast.getVariable().getType().getJvmName());
            if(ast.getValue().isPresent()){
                print(" ");
                print("=");
                print(" ");
                print(ast.getValue());
                print(";");
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        print(ast.getFunction().getReturnType().getJvmName());
        print(" ");
        print(ast.getName());
        print("(");
        int numArgs = ast.getParameters().size()-1;
        int i = 0;
        if(ast.getParameters().size() != 0){
            for (String e: ast.getParameters()) {
                print(e);
                i++;
                if (i != numArgs) {
                    print(", ");
                }
            }
        }
        print(")");
        print(" ");
        print("{");
        if (!ast.getStatements().isEmpty()) {
            //forward indent
            indent++;
            for (Ast.Statement s : ast.getStatements()) {
                newline(indent);
                print(s);
            }
            //back indent
            indent--;
            newline(indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        print(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getVariable().getType().getJvmName());
        print(" ");
        print(ast.getVariable().getJvmName());
        if (ast.getValue().isPresent()) {
            print(" ");
            print("=");
            print(" ");
            print(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getReceiver());
        print(" ");
        print("=");
        print(" ");
        print(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if");
        print(" ");
        print("(");
        print(ast.getCondition());
        print(")");
        print(" ");
        print("{");
        indent++;
        for (Ast.Statement s : ast.getThenStatements()) {
            newline(indent);
            print(s);
        }
        indent--;
        newline(indent);
        print("}");
        if (!ast.getElseStatements().isEmpty()) {
            print(" ");
            print("else");
            print(" ");
            print("{");
            indent++;
            for (Ast.Statement s : ast.getElseStatements()) {
                newline(indent);
                print(s);
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        /*
            switch (letter) {
                case 'y':
                    System.out.print("yes");
                    letter = 'n';
                default:
                    System.out.println("no");
            }
         */
        print("switch ");
        print("(");
        print(ast.getCondition());
        print(")");
        print(" {");
        indent++;
        newline(indent);
        for(int i = 0; i < ast.getCases().size(); i++){
            if(i == ast.getCases().size()-1){
                print("default:");
            }else{
                print("case ");
            }
            visit(ast.getCases().get(i));
            if(i != ast.getCases().size()-1){
                newline(indent);
            }
        }
        indent--;
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        indent++;
        if(ast.getValue().isPresent()){
            print(ast.getValue().get());
            print(":");
        }
        for(Ast.Statement s : ast.getStatements()){
            newline(indent);
            print(s);
        }
        indent--;
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while");
        print(" ");
        print("(");
        print(ast.getCondition());
        print(")");
        print(" ");
        print("{");
        if (ast.getStatements().isEmpty()) {
            print("}");
        } else {
            //indent for after while loop
            indent++;
            for (Ast.Statement s : ast.getStatements()) {
                newline(indent);
                print(s);
            }
            //end the indent for after the last line
            indent--;
            newline(indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return");
        print(" ");
        print(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        //String, char, int, dec
        if (ast.getType() == Environment.Type.STRING) {
            print("\"");
            print(ast.getLiteral());
            print("\"");
        }else if (ast.getType() == Environment.Type.CHARACTER) {
            print("'");
            print(ast.getLiteral());
            print("'");
        }
        else if (ast.getType() == Environment.Type.INTEGER) {
            BigInteger val = BigInteger.class.cast(ast.getLiteral());
            print(val.intValue());
        }
        else if (ast.getType() == Environment.Type.DECIMAL) {
            BigDecimal val = BigDecimal.class.cast(ast.getLiteral());
            print(val.doubleValue());
        }
        else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        print(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        if(ast.getOperator().equals("^")){
            print("Math.pow(");
            print(ast.getLeft());
            print(", ");
            print(ast.getRight());
            print(")");
        }else{
            print(ast.getLeft());
            print(" ");
            if (ast.getOperator().equals("OR")) {
                print("||");
            }
            else if (ast.getOperator().equals("AND")) {
                print("&&");
            }else{
                print(ast.getOperator());
            }
            print(" ");
            print(ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        print(ast.getVariable().getJvmName());
        if (ast.getOffset().isPresent()) {
            print("[");
            print(ast.getOffset().get());
            print("]");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getFunction().getJvmName());
        print("(");
        int numArgs = ast.getArguments().size()-1;
        if(ast.getArguments().size() != 0){
            for (Ast.Expression e: ast.getArguments()) {
                print(e);
                if (0 != numArgs || ast.getArguments().size() != 1) {
                    print(", ");
                }
                numArgs--;
            }
        }
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        int temp = ast.getValues().size()-1;
        for(Ast.Expression e: ast.getValues()){
            print(e);
            if(ast.getValues().size() != 1 && temp!=0){
                print(", ");
            }
            temp--;
        }
        return null;
    }

}