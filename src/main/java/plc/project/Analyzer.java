package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        try {
            Ast.Expression e = ast.getCondition();
            visit(e);
            requireAssignable(Environment.Type.BOOLEAN, e.getType());
            try {
                scope = new Scope(scope);
                for (Ast.Statement s : ast.getStatements()){
                    visit(s);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        catch (RuntimeException exception) {
            throw new RuntimeException(exception);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        try {
            requireAssignable(scope.lookupVariable("returnType").getType(), ast.getValue().getType());
        }catch (RuntimeException exception) {
            throw new RuntimeException(exception);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        //to remove repeating
        Object literal = ast.getLiteral();
        if (literal == Environment.NIL){
            ast.setType(Environment.Type.NIL);
        } else if (literal instanceof Boolean){
            ast.setType(Environment.Type.BOOLEAN);
        }else if (literal instanceof Character){
            ast.setType(Environment.Type.CHARACTER);
        }else if (literal instanceof String) {
            ast.setType(Environment.Type.STRING);
        }else if(literal instanceof BigInteger) {
            BigInteger literalInt = (BigInteger) literal;
            if ((literalInt.intValueExact() > Integer.MAX_VALUE) || (literalInt.intValueExact() < Integer.MIN_VALUE)) {
                throw new RuntimeException("Not in Range");
            }
            ast.setType(Environment.Type.INTEGER);
        }else if (literal instanceof BigDecimal){
            BigDecimal literalDec = (BigDecimal) literal;
            if ((literalDec.doubleValue() == Double.NEGATIVE_INFINITY) || (literalDec.doubleValue() == Double.POSITIVE_INFINITY)) {
                throw new RuntimeException("Not in Range");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        try {
            if (ast.getExpression().getClass() != Ast.Expression.Binary.class){
                throw new RuntimeException("not a binary expression");
            }
        }catch (RuntimeException exception) {
            throw new RuntimeException(exception);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        Ast.Expression l = ast.getLeft();
        Ast.Expression r =ast.getRight();
        visit(l);
        visit(r);
        String operator = ast.getOperator();
        //System.out.println("passed visits");

        if(operator.equals("&&") || operator.equals("||")){
            requireAssignable(Environment.Type.BOOLEAN, l.getType());
            requireAssignable(Environment.Type.BOOLEAN, r.getType());
            ast.setType(Environment.Type.BOOLEAN);
        }else if (operator.equals("<") || operator.equals(">") || operator.equals("==") || operator.equals("!=")){
            requireAssignable(Environment.Type.COMPARABLE, l.getType());
            requireAssignable(Environment.Type.COMPARABLE, r.getType());
            requireAssignable(l.getType(), r.getType());
            ast.setType(Environment.Type.BOOLEAN);
        }else if (operator.equals("+")){
            if(l.getType() == Environment.Type.STRING || r.getType() == Environment.Type.STRING){
                ast.setType(Environment.Type.STRING);
            }else if (l.getType() == Environment.Type.INTEGER && r.getType() == Environment.Type.INTEGER){
                ast.setType(Environment.Type.INTEGER);
            }else if (l.getType() == Environment.Type.DECIMAL && r.getType() == Environment.Type.DECIMAL){
                ast.setType(Environment.Type.DECIMAL);
            }else{
                throw new RuntimeException("Addition of wrong types");
            }
        }else if (operator.equals("-") || operator.equals("*") || operator.equals("/")){
            if (l.getType() == Environment.Type.INTEGER && r.getType() == Environment.Type.INTEGER){
                ast.setType(Environment.Type.INTEGER);
            }else if (l.getType() == Environment.Type.DECIMAL && r.getType() == Environment.Type.DECIMAL){
                ast.setType(Environment.Type.DECIMAL);
            }else{
                throw new RuntimeException("Sub/Mult/Div of wrong types");
            }
        }else if (operator.equals("^")){
            if (l.getType() == Environment.Type.INTEGER && r.getType() == Environment.Type.INTEGER){
                ast.setType(Environment.Type.INTEGER);
            }else if (l.getType() == Environment.Type.DECIMAL && r.getType() == Environment.Type.INTEGER){
                ast.setType(Environment.Type.DECIMAL);
            }else{
                throw new RuntimeException("Power of wrong types");
            }
        }else{
            throw new RuntimeException("No binary");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        try {
            //field
            if (ast.getOffset().isPresent()) {
                Ast.Expression.Access t = (Ast.Expression.Access) ast.getOffset().get();
                t.setVariable(scope.lookupVariable(t.getName()));
                try {
                    String varName = t.getName();
                    scope = scope.lookupVariable(varName).getType().getScope();
                    Environment.Variable var = scope.lookupVariable(ast.getName());
                    ast.setVariable(var);
                }
                finally {
                    scope = scope.getParent();
                }
            }
            //nonfield
            else {
                Environment.Variable var = scope.lookupVariable(ast.getName());
                ast.setVariable(var);
            }
        }
        catch (RuntimeException exception) {
            throw new RuntimeException(exception);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        Environment.Function f = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        List<Ast.Expression> arguments = ast.getArguments();
        List<Environment.Type> types = f.getParameterTypes();
        for (int i = 0; i < arguments.size(); i++) {
            visit(arguments.get(i));
            requireAssignable(types.get(i), arguments.get(i).getType());
        }
        ast.setFunction(f);
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        try{
            for(Ast.Expression e: ast.getValues()){
                //not 100% sure
                requireAssignable(e.getType(), ast.getType());
            }
        }catch (RuntimeException exception) {
            throw new RuntimeException(exception);
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        try {
            if (target != type && target != Environment.Type.ANY && target != Environment.Type.COMPARABLE)
                throw new RuntimeException("Error: Types Do Not Match.");
        }
        catch (RuntimeException re) {
            throw new RuntimeException(re);
        }
    }

}
