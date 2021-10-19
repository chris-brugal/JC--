package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for(Ast.Global global: ast.getGlobals()){
            visit(global);
        }
        Environment.PlcObject mainResult= null;
        for(int i =0; i<ast.getFunctions().size();i++){
            if(ast.getFunctions().get(i).getName().equals("main")){
                mainResult = visit(ast.getFunctions().get(i));
            }else{
                visit(ast.getFunctions().get(i));
            }
        }
                if(mainResult== null){
                    throw new RuntimeException("wrong");
                }
        return mainResult;

    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if (ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));
        } else{
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL );
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        try {
            scope = new Scope(scope);
            ast.getParameters();
            for(Ast.Statement statement: ast.getStatements()){
                visit(statement);
            }
        }finally{
            scope = scope.getParent();
        }
        return Environment.NIL;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), true, visit(ast.getValue().get()));
        } else{
            scope.defineVariable(ast.getName(), true, Environment.NIL );
        }
        return Environment.NIL;

    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
       if(requireType(Boolean.class, visit(ast.getCondition()))){
           try {
               scope = new Scope(scope);
               for(Ast.Statement statement: ast.getThenStatements()){
                   visit(statement);
               }
           }finally{
               scope = scope.getParent();
           }
       } else {
           try {
               scope = new Scope(scope);
               for (Ast.Statement statement : ast.getElseStatements()) {
                   visit(statement);
               }
           } finally {
               scope = scope.getParent();
           }
       }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        Boolean isDefault = true;
        int amountCases=ast.getCases().size();
        for(int i =0; i<amountCases-1;i++){ //we don't iterate on the last item in the case list because that would be the default case, and it doesn't have a value
            if(ast.getCondition().equals(ast.getCases().get(i).getValue())){
                try{
                    isDefault = false;
                    scope = new Scope(scope);
                    for(Ast.Statement statement: ast.getCases().get(i).getStatements()){
                        visit(statement);
                    }
                }finally{
                    scope = scope.getParent();
                }
            }
        }
        if(isDefault){ //since default is the last item in the case list, we just visit the statements for the last case object in getCases()
            try{
                scope = new Scope(scope);
                for(Ast.Statement statement: ast.getCases().get(amountCases-1).getStatements()){
                    visit(statement);
                }
            }finally{
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))){
            try {
                scope = new Scope(scope);
                for(Ast.Statement statement: ast.getStatements()){
                    visit(statement);
                }
            }finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        Ast.Expression exp = ast.getValue();
        return visit(exp);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() != null){
            return Environment.create(ast.getLiteral());
        }else{
            return Environment.NIL;
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        Ast.Expression exp = ast.getExpression();
        return visit(exp);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject l = visit(ast.getLeft());
        Environment.PlcObject r = visit(ast.getRight());
        String op = ast.getOperator();

        if(Objects.equals(op, "&&")){
            //check if right false, left false if neither, else ret true
            if(r.getValue() instanceof Boolean && !(Boolean)r.getValue()){
                //right false
                return Environment.create(false);
            }else if(l.getValue() instanceof Boolean && !(Boolean)l.getValue()){
                //left false
                return Environment.create(false);
            }else{
                if(r.getValue() instanceof Boolean){
                    return Environment.create(true);
                }
            }
        }
        else if (Objects.equals(op, "||")){
            System.out.println("higafhiads");
            //check if right true, left true if neither, else ret false
            if(l.getValue() instanceof Boolean && (Boolean)l.getValue()){
                //right true
                return Environment.create(true);
            }
            if(r.getValue() instanceof Boolean && (Boolean)r.getValue()){
                //left true
                return Environment.create(true);
            }
            if(l.getValue() instanceof Boolean){
                if(r.getValue() instanceof Boolean){
                    return Environment.create(false);
                }
                throw new RuntimeException();
            }
        }
        else if (Objects.equals(op, "<")){
            if(l.getValue() instanceof Comparable){
                //check same type
                if(requireType(l.getValue().getClass(), r) != null){
                    //maybe all inline
                    return Environment.create(((Comparable) l.getValue()).compareTo(r.getValue()) < 0);
                }
            }
        }
        else if (Objects.equals(op, ">")){
            if(l.getValue() instanceof Comparable){
                //check same type
                if(requireType(l.getValue().getClass(), r) != null){
                    return Environment.create(((Comparable) l.getValue()).compareTo(r.getValue()) > 0);
                }
            }
        }
        else if (Objects.equals(op, "==")){
            boolean b = Objects.equals(l.getValue(), r.getValue());
            return Environment.create(b);
        }
        else if (Objects.equals(op, "!=")){
            boolean b = !Objects.equals(l.getValue(), r.getValue());
            return Environment.create(b);
        }
        else if (Objects.equals(op, "+")){
            if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigInteger.class, l).add(requireType(BigInteger.class, r)));
            }else if(l.getValue() instanceof BigDecimal && r.getValue() instanceof BigDecimal){
                return Environment.create(requireType(BigDecimal.class, l).add(requireType(BigDecimal.class, r)));
            }else if(l.getValue() instanceof String || r.getValue() instanceof String){
                return Environment.create(requireType(String.class, l).concat(requireType(String.class, r)));
            }
        }
        else if (Objects.equals(op, "-")){
            if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigInteger.class, l).subtract(requireType(BigInteger.class, r)));
            }else if(l.getValue() instanceof BigDecimal && r.getValue() instanceof BigDecimal){
                return Environment.create(requireType(BigDecimal.class, l).subtract(requireType(BigDecimal.class, r)));
            }
        }
        else if (Objects.equals(op, "*")){
            if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigInteger.class, l).multiply(requireType(BigInteger.class, r)));
            }else if(l.getValue() instanceof BigDecimal && r.getValue() instanceof BigDecimal){
                return Environment.create(requireType(BigDecimal.class, l).multiply(requireType(BigDecimal.class, r)));
            }
        }
        else if (Objects.equals(op, "/")){
            if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger && r.toString().equals("0")){
                //TODO look into this dividing by zero
                throw new Return(Environment.NIL);
            } else if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigInteger.class, l).divide(requireType(BigInteger.class, r)));
            } else if(l.getValue() instanceof BigDecimal && r.getValue() instanceof BigDecimal){
                return Environment.create(requireType(BigDecimal.class, l).divide(requireType(BigDecimal.class, r), RoundingMode.HALF_EVEN));
            }
        }
        else if (Objects.equals(op, "^")){
            //TODO wait for response in MS Teams
        }
        //TODO look into the throwing exceptions for - and + more since there is a left and right value
        throw new Return(Environment.NIL);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent()){
            /*
            TODO not 100% sure abt this
                Figure out how to connect both the scope and the ast expression that was passed in
            */

            Environment.PlcObject off = visit(ast.getOffset().get());
            Environment.PlcObject parent = scope.getParent().lookupVariable(ast.getName()).getValue();
            List<String> a = (List<String>) parent.getValue();
            return Environment.create(a.get(((BigInteger) off.getValue()).intValue()));
        }
        //TODO look into how to throw exceptions***************************************
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        try{
            scope = new Scope(scope);
            List<Environment.PlcObject> params = new ArrayList<>();
            for(int i = 0; i < ast.getArguments().size(); i++){
                Ast.Expression exp = ast.getArguments().get(i);
                params.add(visit(exp));
            }
            return scope.lookupFunction(ast.getName(), params.size()).invoke(params);
        }finally {
            scope = scope.getParent();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        //TODO not 100% sure abt this
        return new Environment.PlcObject(scope, ast.getValues());
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
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
