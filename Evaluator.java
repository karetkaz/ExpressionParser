/**
 * Evaluator for an abstract syntax tree.
 */
public abstract class Evaluator {

	protected static final double[] EMPTY_ARGS = {};

	/**
	 * Invoked on nodes parsed as function call operator(`function(...arguments)`).
	 *
	 * @param function  the name of the function.
	 * @param arguments arguments of the invocation.
	 * @return value of the expression.
	 */
	public abstract double onFunction(String function, double[] arguments) throws Error;

	/**
	 * Convenience method to evaluate constructs where the argument might be an array,
	 * like: `sum(values)`.
	 *
	 * @param function  the name of the function.
	 * @param argument arguments of the invocation.
	 * @return value of the expression.
	 */
	public double onFunction(String function, String argument) throws Error {
		double[] args = {onValue(argument)};
		return onFunction(function, args);
	}

	/**
	 * Invoked on nodes parsed as index operator(`array[subscript]`).
	 *
	 * @param array     the array which is indexed.
	 * @param subscript the subscript to be used.
	 * @return value of the expression.
	 */
	public abstract double onIndex(String array, int subscript) throws Error;

	/**
	 * Convenience method to evaluate constructs where the subscript might be an identifier,
	 * like: `Math[pi]`.
	 *
	 * @param object   the array which is indexed.
	 * @param property the subscript to be used.
	 * @return value of the expression.
	 */
	public double onIndex(String object, String property) throws Error {
		double value = onValue(property);
		int index = (int) value;
		if (value != index) {
			throw new Error("Index expects integer value, got: " + value);
		}
		return onIndex(object, index);
	}

	/**
	 * Invoked on every value node for lookup or to be parsed.
	 *
	 * @param value the variable or number represented as text.
	 * @return value to be used for this token.
	 */
	public double onValue(String value) throws Error {
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new Error("Invalid value or number: `" + value + "`", e);
		}
	}

	/**
	 * Evaluate the expression starting with the given node as root.
	 *
	 * @param node      root of the syntax tree.
	 * @return value of the expression.
	 */
	public double evaluate(Parser.Node node) throws Error {
		double left, right;
		switch (node.getToken()) {
			case Value:
				return onValue(node.getText());

			case Idx:
				if (node.getLeft() == null || node.getRight() == null) {
					// empty index: `[]` or `values[]` or `[values]`
					throw new Error("Invalid operation", node);
				}
				if (node.getLeft().getToken() != Lexer.Token.Value) {
					// invalid array variable: `(9-8)[9]`
					throw new Error("Invalid array operation", node);
				}
				if (node.getRight().getToken() == Lexer.Token.Value) {
					return onIndex(node.getLeft().getText(), node.getRight().getText());
				}

				double value = evaluate(node.getRight());
				int index = (int) value;
				if (value != index) {
					throw new Error("Index expects integer value, got: " + value);
				}
				return onIndex(node.getLeft().getText(), index);

			case Fun:
				if (node.getLeft() == null) {
					if (node.getRight() == null) {
						// empty parenthesis: '()'
						throw new Error("Invalid function call", node);
					}

					// parenthesis: '(3 + 5)'
					return evaluate(node.getRight());
				}

				if (node.getRight() == null) {
					// no arguments: `sum()`
					return onFunction(node.getLeft().getText(), EMPTY_ARGS);
				}

				if (node.getRight().getToken() == Lexer.Token.Value) {
					// single text argument: `sum(array)`
					return onFunction(node.getLeft().getText(), node.getRight().getText());
				}

				int argc = 1;
				for (Parser.Node arg = node.getRight(); arg.getToken() == Lexer.Token.Coma; arg = arg.getRight()) {
					// count how many arguments we have
					argc += 1;
				}

				double[] args = new double[argc];
				Parser.Node arg = node.getRight();
				for (argc = 0; arg.getToken() == Lexer.Token.Coma; arg = arg.getRight()) {
					args[argc] = evaluate(arg.getLeft());
					argc += 1;
				}
				args[argc] = evaluate(arg);

				return onFunction(node.getLeft().getText(), args);

			case Pos:
				right = evaluate(node.getRight());
				return +right;

			case Neg:
				right = evaluate(node.getRight());
				return -right;

			case Cmt:
				right = evaluate(node.getRight());
				if (right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return ~(long) right;

			case Not:
				right = evaluate(node.getRight());
				return right == 0 ? 1 : 0;

			case Pow:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return Math.pow(left, right);

			case Mul:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left * right;

			case Div:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left / right;

			case Rem:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left % right;

			case Add:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left + right;

			case Sub:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left - right;

			case Shl:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left << (long) right;

			case Shr:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left >> (long) right;

			case Sar:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left >>> (long) right;


			case Lt:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left < right ? 1 : 0;

			case Leq:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left <= right ? 1 : 0;

			case Gt:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left > right ? 1 : 0;

			case Geq:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left >= right ? 1 : 0;

			case Eq:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left == right ? 1 : 0;

			case Neq:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				return left != right ? 1 : 0;

			case And:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left & (long) right;

			case Xor:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left ^ (long) right;

			case Ior:
				left = evaluate(node.getLeft());
				right = evaluate(node.getRight());
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left | (long) right;

			case All:
				left = evaluate(node.getLeft());
				// stop on first zero value
				if (left == 0) {
					return left;
				}
				return evaluate(node.getRight());

			case Any:
				left = evaluate(node.getLeft());
				// stop on first non-zero value
				if (left != 0) {
					return left;
				}
				return evaluate(node.getRight());

			case Chk:
				if (node.getRight() == null || node.getRight().getToken() != Lexer.Token.Sel) {
					throw new Error("Invalid operation", node);
				}
				if (evaluate(node.getLeft()) != 0) {
					return evaluate(node.getRight().getLeft());
				}
				return evaluate(node.getRight().getRight());

			default:
				throw new Error("Invalid operation", node);
		}
	}
}
