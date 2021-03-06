package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
        for(Ast.Function function: ast.getFunctions()){
            visit(function);
        }

        return  scope.lookupFunction("main",0).invoke(new ArrayList<>());
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
        Scope sc = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), arguments ->{
            Scope scChild = scope;
            try {
                scope = new Scope(sc);
                for(int i = 0; i < arguments.size(); i++){
                    //not sure about the mutability part (:/)
                    //scope.getParent().lookupVariable(ast.getParameters().get(i)).getMutable()
                    scope.defineVariable(ast.getParameters().get(i), true ,arguments.get(i));
                }
                for(Ast.Statement statement: ast.getStatements()){
                    visit(statement);
                }
            }catch (Return returnValue) {
                return returnValue.value;
            }finally {
                scope = scChild;
            }
            return Environment.NIL;
        });
        return Environment.NIL;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {

        visit(ast.getExpression());

        return Environment.NIL;
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
        if(ast.getReceiver() instanceof Ast.Expression.Access && ast.getReceiver()!=null){
            Environment.PlcObject varVal = visit(ast.getValue());
            if(((Ast.Expression.Access) ast.getReceiver()).getOffset().isPresent()){
                Ast.Expression.Literal offset = (Ast.Expression.Literal) ((Ast.Expression.Access) ast.getReceiver()).getOffset().get();
                Object val = offset.getLiteral();

                Environment.PlcObject temp = scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).getValue();
                List<Object> a = (List<Object>) temp.getValue();

                Ast.Expression.Access as = (((Ast.Expression.Access) ast.getReceiver()));
                String name = as.getName();
                Environment.Variable var = scope.lookupVariable(name);
                a.set(((BigInteger)val).intValue(), varVal.getValue());
                var.setValue(Environment.create(a));
            }else {
                scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).setValue(varVal);
            }

        }else{
            throw new RuntimeException();
        }
        return Environment.NIL;
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
          Object cond=  visit(ast.getCondition()).getValue();

        for(int i =0; i<amountCases-1;i++){ //we don't iterate on the last item in the case list because that would be the default case, and it doesn't have a value
            Ast.Expression.Literal casey= (Ast.Expression.Literal) ast.getCases().get(i).getValue().get();

            if(cond.equals(casey.getLiteral())){
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
        Environment.PlcObject ret = visit(exp);
        throw new Return(ret);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral() != null){
            Object val = ast.getLiteral();
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
        String op = ast.getOperator();

        if(Objects.equals(op, "&&")){
            //check if right false, left false if neither, else ret true
            if(l.getValue() instanceof Boolean && !(Boolean)l.getValue()){
                //left false
                return Environment.create(false);
            }
            Environment.PlcObject r = visit(ast.getRight());
            if(r.getValue() instanceof Boolean && !(Boolean)r.getValue()){
                //right false
                return Environment.create(false);
            }else{
                if(r.getValue() instanceof Boolean){
                    return Environment.create(true);
                }
            }
        }
        else if (Objects.equals(op, "||")){
            //check if right true, left true if neither, else ret false
            if(l.getValue() instanceof Boolean && (Boolean)l.getValue()){
                //right true
                return Environment.create(true);
            }
            Environment.PlcObject r = visit(ast.getRight());
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
            Environment.PlcObject r = visit(ast.getRight());
            if(l.getValue() instanceof Comparable){
                //check same type
                if(requireType(l.getValue().getClass(), r) != null){
                    //maybe all inline
                    return Environment.create(((Comparable) l.getValue()).compareTo(r.getValue()) < 0);
                }
            }
        }
        else if (Objects.equals(op, ">")){
            Environment.PlcObject r = visit(ast.getRight());
            if(l.getValue() instanceof Comparable){
                //check same type
                if(requireType(l.getValue().getClass(), r) != null){
                    return Environment.create(((Comparable) l.getValue()).compareTo(r.getValue()) > 0);
                }
            }
        }
        else if (Objects.equals(op, "==")){
            Environment.PlcObject r = visit(ast.getRight());
            boolean b = Objects.equals(l.getValue(), r.getValue());
            return Environment.create(b);
        }
        else if (Objects.equals(op, "!=")){
            Environment.PlcObject r = visit(ast.getRight());
            boolean b = !Objects.equals(l.getValue(), r.getValue());
            return Environment.create(b);
        }
        else if (Objects.equals(op, "+")){
            Environment.PlcObject r = visit(ast.getRight());
            if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigInteger.class, l).add(requireType(BigInteger.class, r)));
            }else if(l.getValue() instanceof BigDecimal && r.getValue() instanceof BigDecimal){
                return Environment.create(requireType(BigDecimal.class, l).add(requireType(BigDecimal.class, r)));
            }else if(l.getValue() instanceof String || r.getValue() instanceof String){
                return Environment.create(((String)l.getValue()).concat(((String)r.getValue())));
            }
        }
        else if (Objects.equals(op, "-")){
            Environment.PlcObject r = visit(ast.getRight());
            if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigInteger.class, l).subtract(requireType(BigInteger.class, r)));
            }else if(l.getValue() instanceof BigDecimal && r.getValue() instanceof BigDecimal){
                return Environment.create(requireType(BigDecimal.class, l).subtract(requireType(BigDecimal.class, r)));
            }
        }
        else if (Objects.equals(op, "*")){
            Environment.PlcObject r = visit(ast.getRight());
            if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigInteger.class, l).multiply(requireType(BigInteger.class, r)));
            }else if(l.getValue() instanceof BigDecimal && r.getValue() instanceof BigDecimal){
                return Environment.create(requireType(BigDecimal.class, l).multiply(requireType(BigDecimal.class, r)));
            }
        }
        else if (Objects.equals(op, "/")){
            Environment.PlcObject r = visit(ast.getRight());
            if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger && r.toString().equals("0")){
                //TODO look into this dividing by zero throwing a rubntime excep
                throw new RuntimeException();
            } else if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigInteger.class, l).divide(requireType(BigInteger.class, r)));
            } else if(l.getValue() instanceof BigDecimal && r.getValue() instanceof BigDecimal){
                return Environment.create(requireType(BigDecimal.class, l).divide(requireType(BigDecimal.class, r), RoundingMode.HALF_EVEN));
            }
        }
        else if (Objects.equals(op, "^")){
            Environment.PlcObject r = visit(ast.getRight());
            if(l.getValue() instanceof BigInteger && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigInteger.class, l).pow(((BigInteger) r.getValue()).intValue()));
            } else if(l.getValue() instanceof BigDecimal && r.getValue() instanceof BigInteger){
                return Environment.create(requireType(BigDecimal.class, l).pow(((BigInteger) r.getValue()).intValue()));
            }
        }
        //TODO look into the throwing exceptions for - and + more since there is a left and right value
        throw new RuntimeException();
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
            if(((BigInteger) off.getValue()).intValue() < 0 || ((BigInteger) off.getValue()).intValue() >= a.size()){
                throw new RuntimeException();
            }
            return Environment.create(a.get(((BigInteger) off.getValue()).intValue()));
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {

            //scope = new Scope(scope);
            List<Environment.PlcObject> params = new ArrayList<>();
            for(int i = 0; i < ast.getArguments().size(); i++){
                Ast.Expression exp = ast.getArguments().get(i);
                params.add(visit(exp));
            }
            return scope.lookupFunction(ast.getName(), params.size()).invoke(params);

    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        //TODO not 100% sure abt this

        List<Object> items=new ArrayList<>();
        for(Ast.Expression item: ast.getValues()){
          items.add(visit(item).getValue());
        }
        return Environment.create(items);
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
