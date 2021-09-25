package plc.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * See the Parser assignment specification for specific notes on each AST class
 * and how to use it.
 */
public abstract class Ast {

    public static final class Source extends Ast {

        private final List<Global> globals;
        private final List<Function> functions;

        public Source(List<Global> globals, List<Function> functions) {
            this.globals = globals;
            this.functions = functions;
        }

        public List<Global> getGlobals() {
            return globals;
        }

        public List<Function> getFunctions() {
            return functions;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Source &&
                    globals.equals(((Source) obj).globals) &&
                    functions.equals(((Source) obj).functions);
        }

        @Override
        public String toString() {
            return "Ast.Source{" +
                    "globals=" + globals +
                    "functions=" + functions +
                    '}';
        }

    }

    public static final class Global extends Ast {

        private final String name;
        private final boolean mutable;
        private final Optional<Expression> value;

        public Global(String name, boolean mutable, Optional<Expression> value) {
            this.name = name;
            this.mutable = mutable;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public boolean getMutable() {
            return mutable;
        }

        public Optional<Expression> getValue() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Global &&
                    name.equals(((Global) obj).name) &&
                    mutable == ((Global) obj).mutable &&
                    value.equals(((Global) obj).value);
        }

        @Override
        public String toString() {
            return "Ast.Global{" +
                    "name='" + name + '\'' +
                    ", mutable=" + mutable +
                    ", value=" + value +
                    '}';
        }
    }

    public static final class Function extends Ast {

        private final String name;
        private final List<String> parameters;
        private final List<Statement> statements;

        public Function(String name, List<String> parameters, List<Statement> statements) {
            this.name = name;
            this.parameters = parameters;
            this.statements = statements;
        }

        public String getName() {
            return name;
        }

        public List<String> getParameters() {
            return parameters;
        }

        public List<Statement> getStatements() {
            return statements;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Function &&
                    name.equals(((Function) obj).name) &&
                    parameters.equals(((Function) obj).parameters) &&
                    statements.equals(((Function) obj).statements);
        }

        @Override
        public String toString() {
            return "Ast.Function{" +
                    "name='" + name + '\'' +
                    ", parameters=" + parameters +
                    ", statements=" + statements +
                    '}';
        }

    }

    public static abstract class Statement extends Ast {

        public static final class Expression extends Statement {

            private final Ast.Expression expression;

            public Expression(Ast.Expression expression) {
                this.expression = expression;
            }

            public Ast.Expression getExpression() {
                return expression;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Statement.Expression &&
                        expression.equals(((Statement.Expression) obj).expression);
            }

            @Override
            public String toString() {
                return "Ast.Statement.Expression{" +
                        "expression=" + expression +
                        '}';
            }

        }

        public static final class Declaration extends Statement {

            private String name;
            private Optional<Ast.Expression> value;

            public Declaration(String name, Optional<Ast.Expression> value) {
                this.name = name;
                this.value = value;
            }

            public String getName() {
                return name;
            }

            public Optional<Ast.Expression> getValue() {
                return value;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Declaration &&
                        name.equals(((Declaration) obj).name) &&
                        value.equals(((Declaration) obj).value);
            }

            @Override
            public String toString() {
                return "Ast.Statement.Declaration{" +
                        "name='" + name + '\'' +
                        ", value=" + value +
                        '}';
            }

        }

        public static final class Assignment extends Statement {

            private final Ast.Expression receiver;
            private final Ast.Expression value;

            public Assignment(Ast.Expression receiver, Ast.Expression value) {
                this.receiver = receiver;
                this.value = value;
            }

            public Ast.Expression getReceiver() {
                return receiver;
            }

            public Ast.Expression getValue() {
                return value;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Assignment &&
                        receiver.equals(((Assignment) obj).receiver) &&
                        value.equals(((Assignment) obj).value);
            }

            @Override
            public final String toString() {
                return "Ast.Statement.Assignment{" +
                        "receiver=" + receiver +
                        ", value=" + value +
                        '}';
            }

        }

        public static final class If extends Statement {

            private final Ast.Expression condition;
            private final List<Statement> thenStatements;
            private final List<Statement> elseStatements;


            public If(Ast.Expression condition, List<Statement> thenStatements, List<Statement> elseStatements) {
                this.condition = condition;
                this.thenStatements = thenStatements;
                this.elseStatements = elseStatements;
            }

            public Ast.Expression getCondition() {
                return condition;
            }

            public List<Statement> getThenStatements() {
                return thenStatements;
            }

            public List<Statement> getElseStatements() {
                return elseStatements;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof If &&
                        condition.equals(((If) obj).condition) &&
                        thenStatements.equals(((If) obj).thenStatements) &&
                        elseStatements.equals(((If) obj).elseStatements);
            }

            @Override
            public String toString() {
                return "Ast.Statement.If{" +
                        "condition=" + condition +
                        ", thenStatements=" + thenStatements +
                        ", elseStatements=" + elseStatements +
                        '}';
            }

        }

        public static final class Switch extends Statement {

            private final Ast.Expression condition;
            private final List<Case> cases;

            public Switch(Ast.Expression condition, List<Case> cases) {
                this.condition = condition;
                this.cases = cases;
            }

            public Ast.Expression getCondition() {
                return condition;
            }

            public List<Case> getCases() { return cases; }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Switch &&
                        condition.equals(((Switch) obj).condition) &&
                        cases.equals(((Switch) obj).cases);
            }

            @Override
            public String toString() {
                return "Ast.Statement.Switch{" +
                        "condition=" + condition +
                        ", cases=" + cases +
                        '}';
            }

        }

        public static final class Case extends Statement {

            private final Optional<Ast.Expression> value;
            private final List<Statement> statements;

            public Case(Optional<Ast.Expression> value, List<Statement> statements) {
                this.value = value;
                this.statements = statements;
            }

            public Optional<Ast.Expression> getValue() {
                return value;
            }

            public List<Statement> getStatements() {
                return statements;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Case &&
                        value.equals(((Case) obj).value) &&
                        statements.equals(((Case) obj).statements);
            }

            @Override
            public String toString() {
                return "Ast.Statement.Case{" +
                        "value=" + value +
                        ", statements=" + statements +
                        '}';
            }

        }

        public static final class While extends Statement {

            private final Ast.Expression condition;
            private final List<Statement> statements;

            public While(Ast.Expression condition, List<Statement> statements) {
                this.condition = condition;
                this.statements = statements;
            }

            public Ast.Expression getCondition() {
                return condition;
            }

            public List<Statement> getStatements() {
                return statements;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof While &&
                        condition.equals(((While) obj).condition) &&
                        statements.equals(((While) obj).statements);
            }

            @Override
            public String toString() {
                return "Ast.Statement.While{" +
                        "condition=" + condition +
                        ", statements=" + statements +
                        '}';
            }

        }

        public static final class Return extends Statement {

            private final Ast.Expression value;

            public Return(Ast.Expression value) {
                this.value = value;
            }

            public Ast.Expression getValue() {
                return value;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Return &&
                        value.equals(((Return) obj).value);
            }

            @Override
            public String toString() {
                return "Ast.Stmt.Return{" +
                        "value=" + value +
                        '}';
            }

        }

    }

    public static abstract class Expression extends Ast {

        public static final class Literal extends Expression {

            private final Object literal;

            public Literal(Object literal) {
                this.literal = literal;
            }

            public Object getLiteral() {
                return literal;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Literal &&
                        Objects.equals(literal, ((Literal) obj).literal);
            }

            @Override
            public String toString() {
                return "Ast.Expression.Literal{" +
                        "literal=" + literal +
                        '}';
            }

        }

        public static final class Group extends Expression {

            private final Expression expression;

            public Group(Expression expression) {
                this.expression = expression;
            }

            public Expression getExpression() {
                return expression;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Group &&
                        expression.equals(((Group) obj).expression);
            }

            @Override
            public String toString() {
                return "Ast.Expression.Group{" +
                        "expression=" + expression +
                        '}';
            }

        }

        public static final class Binary extends Expression {

            private final String operator;
            private final Expression left;
            private final Expression right;

            public Binary(String operator, Expression left, Expression right) {
                this.operator = operator;
                this.left = left;
                this.right = right;
            }

            public String getOperator() {
                return operator;
            }

            public Expression getLeft() {
                return left;
            }

            public Expression getRight() {
                return right;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Binary &&
                        operator.equals(((Binary) obj).operator) &&
                        left.equals(((Binary) obj).left) &&
                        right.equals(((Binary) obj).right);
            }

            @Override
            public String toString() {
                return "Ast.Expression.Binary{" +
                        "operator='" + operator + '\'' +
                        ", left=" + left +
                        ", right=" + right +
                        '}';
            }

        }

        public static final class Access extends Expression {

            private final Optional<Expression> offset;
            private final String name;

            public Access(Optional<Expression> offset, String name) {
                this.offset = offset;
                this.name = name;
            }

            public Optional<Expression> getOffset() {
                return offset;
            }

            public String getName() {
                return name;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Access &&
                        offset.equals(((Access) obj).offset) &&
                        name.equals(((Access) obj).name);
            }

            @Override
            public String toString() {
                return "Ast.Expr.Access{" +
                        "offset=" + offset +
                        ", name='" + name + '\'' +
                        '}';
            }

        }

        public static final class Function extends Expression {

            private final String name;
            private final List<Expression> arguments;

            public Function(String name, List<Expression> arguments) {
                this.name = name;
                this.arguments = arguments;
            }

            public String getName() {
                return name;
            }

            public List<Expression> getArguments() {
                return arguments;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof Expression.Function &&
                        name.equals(((Expression.Function) obj).name) &&
                        arguments.equals(((Expression.Function) obj).arguments);
            }

            @Override
            public String toString() {
                return "Ast.Expression.Function{" +
                        ", name='" + name + '\'' +
                        ", arguments=" + arguments +
                        '}';
            }

        }

        public static final class PlcList extends Expression {

            private final List<Expression> values;

            public PlcList(List<Expression> values) {
                this.values = values;
            }

            public List<Expression> getValues() {
                return values;
            }

            @Override
            public boolean equals(Object obj) {
                return obj instanceof PlcList &&
                        values.equals(((PlcList) obj).values);
            }

            @Override
            public String toString() {
                return "Ast.Expression.PlcList{" +
                        ", values=[" + values + "]" +
                        '}';
            }

        }

    }

}
