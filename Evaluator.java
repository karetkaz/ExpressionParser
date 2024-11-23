/**
 * Evaluator for an abstract syntax tree.
 */
public abstract class Evaluator {

	protected static final double[] EMPTY_ARGS = {};

	/**
	 * Invoked on every value node for lookup or to be parsed.
	 *
	 * @param value the variable or number represented as text.
	 * @return value to be used for this token.
	 */
	protected abstract double onValue(String value) throws Error;

	/**
	 * Invoked on nodes parsed as index operator(`array[subscript]`).
	 *
	 * @param array     the array which is indexed.
	 * @param subscript the subscript to be used.
	 * @return value of the expression.
	 */
	protected abstract double onArray(String array, int subscript) throws Error;

	/**
	 * Invoked on nodes parsed as function call operator(`function(...arguments)`).
	 *
	 * @param function  the name of the function.
	 * @param arguments arguments of the invocation.
	 * @return value of the expression.
	 */
	protected abstract double onFunction(String function, double[] arguments) throws Error;

	/**
	 * Convenience method to evaluate constructs where the subscript might be an identifier,
	 * like: `Math[pi]`.
	 *
	 * @param array   the array which is indexed.
	 * @param subscript the subscript to be used.
	 * @return value of the expression.
	 */
	protected double onArray(String array, Parser.Node subscript) throws Error {
		double value = evaluate(subscript);
		int index = (int) value;
		if (value != index) {
			throw new Error("Invalid integer subscript", subscript);
		}
		return onArray(array, index);
	}

	/**
	 * Convenience method to evaluate constructs where the argument might be an array,
	 * like: `sum(values)`.
	 *
	 * @param function  the name of the function.
	 * @param arguments arguments of the invocation.
	 * @return value of the expression.
	 */
	protected double onFunction(String function, Parser.Node arguments) throws Error {
		if (arguments == null) {
			return onFunction(function, EMPTY_ARGS);
		}

		int n = 1;
		for (Parser.Node node = arguments; node.token == Lexer.Token.Coma; n += 1) {
			if (Lexer.Token.Coma.right2left) {
				node = node.right;
			} else {
				node = node.left;
			}
		}

		double[] args = new double[n];
		evaluateArguments(args, 0, arguments);
		return onFunction(function, args);
	}

	private int evaluateArguments(double[] args, int pos, Parser.Node arguments) throws Error {
		if (arguments.token != Lexer.Token.Coma) {
			args[pos] = evaluate(arguments);
			return pos;
		}
		if (Lexer.Token.Coma.right2left) {
			evaluateArguments(args, pos + 1, arguments.right);
			args[pos] = evaluate(arguments.left);
		} else {
			pos = evaluateArguments(args, pos, arguments.left) + 1;
			args[pos] = evaluate(arguments.right);
		}
		return pos;
	}

	/**
	 * Evaluate the expression starting with the given node as root.
	 *
	 * @param node      root of the syntax tree.
	 * @return value of the expression.
	 */
	public double evaluate(Parser.Node node) throws Error {
		double left, right;
		switch (node.token) {
			case Value:
				try {
					return onValue(node.getText());
				} catch (Error e) {
					throw e;
				} catch (Exception e) {
					throw new Error("Invalid value", node, e);
				}

			case Fun:
				if (node.left == null && node.right == null) {
					// empty parenthesis: `()`
					throw new Error("Invalid function call", node);
				}
				if (node.left == null) {
					// (3 + 2)
					return evaluate(node.right);
				}
				if (node.left.token != Lexer.Token.Value) {
					// invalid function name: `3-9()`
					throw new Error("Invalid function call", node);
				}
				return onFunction(node.left.getText(), node.right);

			case Idx:
				if (node.left == null || node.right == null) {
					// empty index: `[]` or `values[]` or `[values]`
					throw new Error("Invalid array subscript", node);
				}
				if (node.left.token != Lexer.Token.Value) {
					// invalid array variable: `(9-8)[9]`
					throw new Error("Invalid array subscript", node);
				}
				return onArray(node.left.getText(), node.right);

			case Pos:
				right = evaluate(node.right);
				return +right;

			case Neg:
				right = evaluate(node.right);
				return -right;

			case Cmt:
				right = evaluate(node.right);
				if (right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return ~(long) right;

			case Not:
				right = evaluate(node.right);
				return right == 0 ? 1 : 0;

			case Pow:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return Math.pow(left, right);

			case Mul:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left * right;

			case Div:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left / right;

			case Rem:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left % right;

			case Add:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left + right;

			case Sub:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left - right;

			case Shl:
				left = evaluate(node.left);
				right = evaluate(node.right);
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left << (long) right;

			case Shr:
				left = evaluate(node.left);
				right = evaluate(node.right);
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left >>> (long) right;

			case Sar:
				left = evaluate(node.left);
				right = evaluate(node.right);
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left >> (long) right;

			case Lt:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left < right ? 1 : 0;

			case Leq:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left <= right ? 1 : 0;

			case Gt:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left > right ? 1 : 0;

			case Geq:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left >= right ? 1 : 0;

			case Eq:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left == right ? 1 : 0;

			case Neq:
				left = evaluate(node.left);
				right = evaluate(node.right);
				return left != right ? 1 : 0;

			case And:
				left = evaluate(node.left);
				right = evaluate(node.right);
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left & (long) right;

			case Xor:
				left = evaluate(node.left);
				right = evaluate(node.right);
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left ^ (long) right;

			case Ior:
				left = evaluate(node.left);
				right = evaluate(node.right);
				if (left != (long) left || right != (long) right) {
					throw new Error("Invalid integer operation", node);
				}
				return (long) left | (long) right;

			case All:
				left = evaluate(node.left);
				if (left == 0) {
					// stop at the first zero value
					return left;
				}
				return evaluate(node.right);

			case Any:
				left = evaluate(node.left);
				if (left != 0) {
					// stop at the first non-zero value
					return left;
				}
				return evaluate(node.right);

			case Chk:
				if (node.right == null || node.right.token != Lexer.Token.Sel) {
					throw new Error("Invalid operation", node);
				}
				if (evaluate(node.left) != 0) {
					return evaluate(node.right.left);
				}
				return evaluate(node.right.right);

		}
		throw new Error("Invalid operation", node);
	}
}
