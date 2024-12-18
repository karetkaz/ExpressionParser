public class Test {
	private static final double epsilon = 0;
	private static final double epsilonTrig = 1e-15;

	private static final double x = Math.random() * 100;
	private static final double y = x + Math.random() * 100;
	private static final double z = y + Math.random() * 100;
	private static final double w = Math.random() * -100;
	private static final double[] vec = {x, y, z, w};

	public static void main(String[] args) throws Exception {
		testValues();
		testBitwise();
		testArithmetic();
		testPowers();
		testEquality();
		testPrecedence();
		testShortCircuit();
		testPrimary();

		// test recursive parser
		assertEquals(3, "0?2:3", epsilon, false, "0 3 ?");
		assertEquals(2, "1?2:3", epsilon, false, "1 2 ?");
		assertEquals(x < 0 ? -x : x, "x < 0 ? -x : x", epsilon, false, "x 0 < x ?");
		assertEquals(w < 0 ? -w : w, "w < 0 ? -w : w", epsilon, false, "w 0 < w - ?");
		assertEquals(x < y ? x : y, "x < y ? x : y", epsilon, false, "x y < x ?");
		assertEquals(x > y ? x : y, "x > y ? x : y", epsilon, false, "x y > y ?");
		assertEquals(x < y ? y - x : x - y, "x < y ? y - x : x - y", epsilon, false, "x y < y x - ?");
		assertEquals(x < y ? y - x * w : w * x - y, "x < y ? y - x * w : w * x - y", epsilon, false, "x y < y x w * - ?");
		assertEquals(x, "(x)", epsilon, false, "x");

		assertError("3x", "Invalid value: `3x`, position: 0, token: Value");
		assertError("3.1415.3", "Invalid value: `3.1415.3`, position: 0, token: Value");
		assertError("(0 : 50) ? x : y", "Invalid operation: `:`, position: 3, token: Sel");
		assertError("(0 ? 50) ? x : y", "Invalid operation: `?`, position: 3, token: Chk");
		assertError("~3.14", "Invalid integer operation: `~`, position: 0, token: Cmt");
		assertError("3.14 & 1", "Invalid integer operation: `&`, position: 5, token: And");
		assertError("min()", "At least one argument expected");
		assertError("pow()", "Two argument expected");
		assertError("pow(1, 2, 3)", "Two argument expected");
		assertError("3()", "Invalid function: 3");
		assertError("3[0]", "Invalid subscript: 3[0]");
		assertError("w(0)", "Invalid function: w");
		assertError("w[0]", "Invalid subscript: w[0]");
		assertError("-3()", "Invalid function call: `(`, position: 2, token: Fun");
		assertError("-3[0]", "Invalid array subscript: `[`, position: 2, token: Idx");
		assertError("(-3)()", "Invalid function call: `(`, position: 4, token: Fun");
		assertError("(-3)[0]", "Invalid array subscript: `[`, position: 4, token: Idx");
		assertError("vec[0]()", "Invalid function call: `(`, position: 6, token: Fun");
		assertError("x(x())", "Invalid function: x");
		assertError("x[x[]]", "Unary operator expected: `]`, position: 4, token: RBracket");
		assertError("vec()", "Invalid function: vec");
		assertError("vec[]", "Unary operator expected: `]`, position: 4, token: RBracket");
		assertError("[x]", "Invalid array subscript: `[`, position: 0, token: Idx");
		assertError("(vec)", "Invalid value: `vec`, position: 1, token: Value");
		assertError("[vec]", "Invalid array subscript: `[`, position: 0, token: Idx");
		assertError("()", "Invalid function call: `(`, position: 0, token: Fun");
		assertError("[]", "Unary operator expected: `]`, position: 1, token: RBracket");
		assertError("(([([])]))", "Unary operator expected: `]`, position: 5, token: RBracket");
		assertError("(([([])]))", "Unary operator expected: `]`, position: 5, token: RBracket");
	}

	public static void testValues() throws Exception {
		assertEquals(4, "4", epsilon, false, "4");
		assertEquals(4, "4.", epsilon, false, "4.");
		assertEquals(4, "4.000", epsilon, false, "4.000");
		assertEquals(Math.E, "e", epsilon, false, "e");
		assertEquals(x, "x", epsilon, false, "x");
		assertEquals(y, "y", epsilon, false, "y");
		assertEquals(z, "z", epsilon, false, "z");
		assertEquals(w, "w", epsilon, false, "w");
		assertEquals(Math.PI, "pi", epsilon, false, "pi");
		assertEquals(Math.PI / 4, "pi / 4", epsilon, false, "pi 4 /");
	}

	public static void testBitwise() throws Exception {
		assertEquals(~4, "~4", epsilon, false, "4 ~");

		assertEquals(27 & 89, "27 & 89", epsilon, false, "27 89 &");
		assertEquals(27 | 9, "27 | 9", epsilon, false, "27 9 |");
		assertEquals(27 ^ 63, "27 ^ 63", epsilon, false, "27 63 ^");
		assertEquals(-1L >>> 30, "-1 >>> 30", epsilon, false, "1 - 30 >>>");
		assertEquals(23 >> 5, "23 >> 5", epsilon, false, "23 5 >>");
		assertEquals(23 << 5, "23 << 5", epsilon, false, "23 5 <<");
		assertEquals(3101, "198475 >>> 6", epsilon, false, "198475 6 >>>");
	}

	public static void testArithmetic() throws Exception {
		assertEquals(-4, "-4", epsilon, false, "4 -");

		assertEquals(27 + 89, "27 + 89", epsilon, false, "27 89 +");
		assertEquals(27 - 9, "27 - 9", epsilon, false, "27 9 -");
		assertEquals(5.7 * 5, "5.7 * 5", epsilon, false, "5.7 5 *");
		assertEquals(23. / 5, "23 / 5", epsilon, false, "23 5 /");
		assertEquals(6436343, "23 ** 5", epsilon, false, "23 5 **");

		assertEquals(3 - 2 - 1, "3 - 2 - 1", epsilon, false, "3 2 - 1 -");
		assertEquals(5. / 4 / 3 / 2, "5 / 4 / 3 / 2", epsilon, false, "5 4 / 3 / 2 /");
		assertEquals(1 + 2 * 3, "1 + 2 * 3", epsilon, false, "1 2 3 * +");
		assertEquals((1 + 2) * 3, "(1 + 2) * 3", epsilon, false, "1 2 + 3 *");
	}

	public static void testEquality() throws Exception {
		assertEquals(1, "1 == 1", epsilon, false, "1 1 ==");
		assertEquals(0, "1 <> 1", epsilon, false, "1 1 <>");
		assertEquals(0, "1 == 0", epsilon, false, "1 0 ==");
		assertEquals(1, "1 <> 0", epsilon, false, "1 0 <>");

		assertEquals(1, "1 < 2", epsilon, false, "1 2 <");
		assertEquals(0, "1 < 1", epsilon, false, "1 1 <");
		assertEquals(0, "1 < 0", epsilon, false, "1 0 <");

		assertEquals(0, "1 > 2", epsilon, false, "1 2 >");
		assertEquals(0, "1 > 1", epsilon, false, "1 1 >");
		assertEquals(1, "1 > 0", epsilon, false, "1 0 >");

		assertEquals(1, "1 <= 2", epsilon, false, "1 2 <=");
		assertEquals(1, "1 <= 1", epsilon, false, "1 1 <=");
		assertEquals(0, "1 <= 0", epsilon, false, "1 0 <=");

		assertEquals(0, "1 >= 2", epsilon, false, "1 2 >=");
		assertEquals(1, "1 >= 1", epsilon, false, "1 1 >=");
		assertEquals(1, "1 >= 0", epsilon, false, "1 0 >=");

		assertEquals(0, "(1. + 2.) + 3. <> 1. + (2. + 3.)", epsilon, false, "1. 2. + 3. + 1. 2. 3. + + <>");
		assertEquals(1, "(.1 + .2) + .3 <> .1 + (.2 + .3)", epsilon, false, ".1 .2 + .3 + .1 .2 .3 + + <>");
	}

	public static void testPrecedence() throws Exception {
		// '-' associates right to left
		assertEquals(1, "----1", epsilon, true, "(((1 -) -) -) -");
		assertEquals(1, "++++1", epsilon, true, "(((1 +) +) +) +");
		assertEquals(1, "~~~~1", epsilon, true, "(((1 ~) ~) ~) ~");
		assertEquals(1, "!!!!1", epsilon, true, "(((1 !) !) !) !");
		assertEquals(8, "3 ----5", epsilon, false, "3 5 - - - -");
		assertEquals(8, "3 ++++5", epsilon, false, "3 5 + + + +");
		assertEquals(4, "3 +-~!5", epsilon, false, "3 5 ! ~ - +");
		assertEquals(9, "3 -~~~5", epsilon, false, "3 5 ~ ~ ~ -");
		assertEquals(3, "3 -!!!5", epsilon, false, "3 5 ! ! ! -");
		assertEquals(8, "--3 +--5", epsilon, false, "3 - - 5 - - +");
		assertEquals(8, "++3 +++5", epsilon, false, "3 + + 5 + + +");
		assertEquals(3, "3 ----5 - 5", epsilon, false, "3 5 - - - - 5 -");
		assertEquals(3, "3 ++++5 - 5", epsilon, false, "3 5 + + + + 5 -");

		// ',' associates right to left
		assertEquals(1, "min(1, 2, 3)", epsilon, true, null);// todo "1 2 3 min(#3)");

		// all binary operators are left to right
		assertEquals(6, "1 + 2 + 3", epsilon, true, "(1 2 +) 3 +");


		// '*' has higher precedence than '+'
		assertEquals(7, "1 + 2 * 3", epsilon, true, "1 (2 3 *) +");
		assertEquals(5, "1 * 2 + 3", epsilon, true, "(1 2 *) 3 +");

		// force evaluate '+' first
		assertEquals(9, "(1 + 2) * 3", epsilon, true, "(1 2 +) 3 *");

		assertEquals(1, "1 < 2 || 2 < 1", epsilon, true, "(1 2 <) ||");
		assertEquals(0, "1 < 2 && 2 < 1", epsilon, true, "(1 2 <) (2 1 <) &&");
	}

	public static void testShortCircuit() throws Exception {
		assertEquals(2, "1 && 2", epsilon, false, "1 2 &&");
		assertEquals(0, "0 && 2", epsilon, false, "0 &&");
		assertEquals(0, "1 && 0", epsilon, false, "1 0 &&");
		assertEquals(1, "1 || 2", epsilon, false, "1 ||");
		assertEquals(2, "0 || 2", epsilon, false, "0 2 ||");
		assertEquals(1, "1 || 0", epsilon, false, "1 ||");
		assertEquals(0, "1 < 2 && 2 < 1", epsilon, false, "1 2 < 2 1 < &&");
		assertEquals(0, "2 < 1 && 1 < 2", epsilon, false, "2 1 < &&");
		assertEquals(1, "1 < 2 || 2 < 1", epsilon, false, "1 2 < ||");
		assertEquals(1, "2 < 1 || 1 < 2", epsilon, false, "2 1 < 1 2 < ||");
		assertEquals(1, "1 < 2 && 2 < 3 && 3 < 4", epsilon, false, "1 2 < 2 3 < && 3 4 < &&");
		assertEquals(1, "1 < 2 || 2 < 3 || 3 < 4", epsilon, false, "1 2 < || ||");

		// test short-circuiting && stops on first zero value
		assertEquals(3, "1 && 2 && 3", epsilon, false, "1 2 && 3 &&");
		assertEquals(0, "1 && 2 && 0", epsilon, false, "1 2 && 0 &&");
		assertEquals(0, "1 && 0 && 3", epsilon, false, "1 0 && &&");
		assertEquals(0, "0 && 2 && 3", epsilon, false, "0 && &&");

		// test short-circuiting || stops on first non-zero value
		assertEquals(1, "1 || 2 || 3", epsilon, false, "1 || ||");
		assertEquals(1, "1 || 2 || 0", epsilon, false, "1 || ||");
		assertEquals(1, "1 || 0 || 3", epsilon, false, "1 || ||");
		assertEquals(2, "0 || 2 || 3", epsilon, false, "0 2 || ||");

		assertEquals(1, "1 < 2 && 2 < 3 && 3 < 4", epsilon, false, "1 2 < 2 3 < && 3 4 < &&");
		assertEquals(1, "1 < 2 || 2 < 3 || 3 < 4", epsilon, false, "1 2 < || ||");
	}

	public static void testPrimary() throws Error {
		assertEquals(4, "(4)", epsilon, false, "4");
		assertEquals(2, "(4 / 2)", epsilon, false, "4 2 /");
		assertEquals(0, "abs(0)", epsilon, false, "0 abs(#1)");
		assertEquals(1, "abs(1)", epsilon, false, "1 abs(#1)");
		assertEquals(1, "abs(-1)", epsilon, false, null);// todo "1 - abs(#1)");
		assertEquals(0, "sin(0)", epsilon, false, "0 sin(#1)");
		assertEquals(1, "cos(0)", epsilon, false, "0 cos(#1)");
		assertEquals(0, "sin(pi)", epsilonTrig, false, "pi sin(#1)");
		assertEquals(-1, "cos(pi)", epsilon, false, "pi cos(#1)");
		assertEquals(1, "sin(pi / 2)", epsilon, false, null);// todo "pi 2 / sin(#1)");
		assertEquals(0, "cos(pi / 2)", epsilonTrig, false, null);// todo "pi 2 / cos(#1)");
		assertEquals(-1, "sin(3 * pi / 2)", epsilon, false, null);// todo "3 pi * 2 / sin(#1)");
		assertEquals(0, "cos(3 * pi / 2)", epsilonTrig, false, null);// todo "3 pi * 2 / cos(#1)");
		assertEquals(.4, "min(1., .9, 4., .4, .8)", epsilon, false, null);// todo "1. .9 4. .4 .8 min(#5)");
		assertEquals(4., "max(1., .9, 4., .4, .8)", epsilon, false, null);// todo "1. .9 4. .4 .8 max(#5)");
		assertEquals(22.8, "avg(100., 2., 3., 4., 5)", epsilon, false, null);// todo "100. 2. 3. 4. 5 avg(#5)");
		assertEquals(.3, "min(.8, max(1., .9, 4., .4, .8), .4, max(.1, .3, .2), .4, max(1., .9, 4., .4, .8))", epsilon, false, null);// todo ".8 1. .9 4. .4 .8 max(#5) .4 .1 .3 .2 max(#3) .4 1. .9 4. .4 .8 max(#5) min(#6)");

		assertEquals(x, "vec[0]", epsilon, false, "0 vec[]");
		assertEquals(y, "vec[1]", epsilon, false, "1 vec[]");
		assertEquals(z, "vec[2]", epsilon, false, "2 vec[]");
		assertEquals(w, "vec[3]", epsilon, false, "3 vec[]");
		assertError("vec[4]", "Index 4 out of bounds for length 4");

		assertEquals(x, "vec[x]", epsilon, false, null);// todo "x vec[]");
		assertEquals(y, "vec[y]", epsilon, false, null);// todo "y vec[]");
		assertEquals(z, "vec[z]", epsilon, false, null);// todo "z vec[]");
		assertEquals(w, "vec[w]", epsilon, false, null);// todo "w vec[]");
		assertError("vec[a]", "Invalid value: `a`, position: 4, token: Value");

		assertEquals(w, "min(vec[0], vec[1], vec[2], vec[3])", epsilon, false, null);// todo "vec[0] vec[1] vec[2] vec[3] min(#4)");
		assertEquals(x, "min(vec[0], vec[1], vec[2])", epsilon, false, null);// todo "vec[0] vec[1] vec[2] min(#3)");
		assertEquals(x, "min(vec[0], vec[1])", epsilon, false, null);// todo "vec[0] vec[1] min(#2)");
		assertEquals(x, "min(vec[0])", epsilon, false, null);// todo "vec[0] min(#1)");

		assertEquals(w, "min(vec[x], vec[y], vec[z], vec[w])", epsilon, false, null);// todo "vec[x] vec[y] vec[z] vec[w] min(#4)");
		assertEquals(x, "min(vec[x], vec[y], vec[z])", epsilon, false, null);// todo "vec[x] vec[y] vec[z] min(#3)");
		assertEquals(x, "min(vec[x], vec[y])", epsilon, false, null);// todo "vec[x] vec[y] min(#2)");
		assertEquals(x, "min(vec[x])", epsilon, false, null);// todo "vec[x] min(#1)");

		assertEquals(w, "min(vec)", epsilon, false, null);// todo "vec min(#1)");
	}

	public static void testPowers() throws Exception {
		if (!Lexer.Token.Pow.right2left) {
			// Pow should be right to left associative, with higher precedence than unary operators:
			// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Exponentiation
			assertEquals(Math.pow(Math.pow(2, 3), 4), "2 ** 3 ** 4", epsilon, false, "2 3 ** 4 **");
			assertEquals(4, "-2 ** 2", epsilon, false, "2 - 2 **");
			assertEquals(4, "(-2) ** 2", epsilon, false, "2 - 2 **");
			assertEquals(.25, "-2 ** -2", epsilon, false, "2 - 2 - **");
			assertEquals(.25, "(-2) ** -2", epsilon, false, "2 - 2 - **");
			return;
		}

		assertEquals(Math.pow(2, Math.pow(3, 4)), "2 ** 3 ** 4", epsilon, false, "2 3 4 ** **");
		assertEquals(4, "2 ** 2", epsilon, false, "2 2 **");
		assertEquals(.25, "2 ** -2", epsilon, false, "2 2 - **");

		assertError("-2 ** 2", "Precedence error, consider using parenthesis: `**`, position: 3, token: Pow");
		assertEquals(-4, "-(2 ** 2)", epsilon, false, "2 2 ** -");
		assertError("-2 ** -2", "Precedence error, consider using parenthesis: `**`, position: 3, token: Pow");
		assertEquals(-.25, "-(2 ** -2)", epsilon, false, "2 2 - ** -");
	}

	private static void assertError(String expression, String message) throws Error {
		try {
			assertEquals(Double.NaN, expression, 0, false, null);
		} catch (Exception e) {
			if (message != null && message.equals(e.getMessage())) {
				return;
			}
			throw e;
		}
		throw new Error("Error expected");
	}

	private static void assertEquals(double expected, String expression, double epsilon, boolean group, String rpn) throws Error {
		Parser.Node root = Parser.parse(expression);
		Evaluator evaluator = new EvaluatorExt(root, group);
		double value = evaluator.evaluate(root);
		String evaluation = evaluator.toString();

		if (!(Math.abs(expected - value) <= epsilon)) {
			if (!(Double.isNaN(expected) && Double.isNaN(value))) {
				throw new Error("Expected value: `" + expected + "`, got: `" + value + "`");
			}
		}
		if (rpn != null && !rpn.contentEquals(evaluation)) {
			throw new Error("Expected value: `" + rpn + "`, got: `" + evaluation + "`");
		}
		if (!compare(root, ParserIterative.parse(new Lexer(expression)))) {
			throw new Error("Expected same tree for recursive and iterative parsing");
		}
		/*System.out.println("`" + expression + "` := " + value
				+ "\n\tparanthesis: " + Visitor.format(root, true, false)
				+ "\n\tevaluation: " + evaluation
		);*/
	}

	private static boolean compare(Parser.Node a, Parser.Node b) throws Error {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (a.getToken() != b.getToken()) {
			return false;
		}
		if (a.getLeft() != null && !compare(a.getLeft(), b.getLeft())) {
			return false;
		}
		if (a.getRight() != null && !compare(a.getRight(), b.getRight())) {
			return false;
		}

		if (!a.getText().equals(b.getText())) {
			throw new Error("Text does not match");
		}
		if (a.getPosition() != b.getPosition()) {
			throw new Error("Position does not match");
		}
		return true;
	}

	/**
	 * Evaluator which appends to the out buffer the evaluated values (in reverse polish notation)
	 * except for short-circuiting operators, where only the evaluated values are printed
	 * and also allows invalid structures like: `[]`, `()`, `[x]`, `x[]`, ...
	 */
	static class EvaluatorExt extends EvaluatorMath {
		final StringBuilder out = new StringBuilder();
		private final Parser.Node root;
		private final boolean group;

		public EvaluatorExt(Parser.Node root, boolean group) {
			this.root = root;
			this.group = group;
		}

		@Override
		protected double onValue(String value) throws Error {
			switch (value) {
				case "x":
					return x;

				case "y":
					return y;

				case "z":
					return z;

				case "w":
					return w;
			}
			return super.onValue(value);
		}

		@Override
		protected double onArray(String array, int subscript) throws Error {
			if ("vec".equals(array)) {
				// enable vec[0], vec[1], vec[2], vec[3], ...
				return vec[subscript];
			}
			return super.onArray(array, subscript);
		}

		@Override
		protected double onArray(String array, Parser.Node property) throws Error {
			if ("vec".equals(array)) {
				// enable vec[x], vec[y], vec[z], vec[w]
				switch (property.getText()) {
					case "x":
						return vec[0];

					case "y":
						return vec[1];

					case "z":
						return vec[2];

					case "w":
						return vec[3];
				}
			}
			return super.onArray(array, property);
		}

		@Override
		protected double onFunction(String function, Parser.Node arguments) throws Error {
			if (arguments != null && arguments.getText().equals("vec")) {
				// treat `vec` as an array: min(vec), max(vec), ...
				return onFunction(function, vec);
			}
			return super.onFunction(function, arguments);
		}

		static int countArgs(Parser.Node node) {
			if (node == null) {
				return 0;
			}
			int count = 1;
			while (node.getToken() == Lexer.Token.Coma) {
				if (Lexer.Token.Coma.right2left) {
					node = node.getRight();
				} else {
					node = node.getLeft();
				}
				count += 1;
			}
			return count;
		}

		@Override
		public double evaluate(Parser.Node node) throws Error {
			if (node.getToken() == Lexer.Token.Fun && node.getLeft() == null) {
				return super.evaluate(node);
			}

			if (node.getToken() == Lexer.Token.Fun && node.getLeft() != null) {
				double value = super.evaluate(node);
				out.append(node.getLeft().getText()).append("(#").append(countArgs(node.getRight())).append(") ");
				return value;
			}

			if (node.getToken() == Lexer.Token.Idx && node.getLeft() != null) {
				double value = super.evaluate(node);
				out.append(node.getLeft().getText()).append("[] ");
				return value;
			}

			if (group && node != root) {
				if (node.getLeft() != null || node.getRight() != null) {
					out.append("(");
				}
			}

			double value = super.evaluate(node);
			out.append(node.getText());

			if (group && node != root) {
				if (node.getLeft() != null || node.getRight() != null) {
					out.append(")");
				}
			}
			if (node != root) {
				out.append(" ");
			}
			return value;
		}

		@Override
		public String toString() {
			return out.toString().trim();
		}
	}

	static class EvaluatorMath extends Evaluator {

		@Override
		protected double onValue(String value) throws Error {
			switch (value) {
				case "inf":
					return inf;

				case "pi":
					return pi;

				case "e":
					return e;
			}
			return Double.parseDouble(value);
		}

		@Override
		protected double onArray(String array, int subscript) throws Error {
			throw new Error("Invalid subscript: " + array + "[" + subscript + "]");
		}

		@Override
		protected double onFunction(String function, double[] arguments) throws Error {
			switch (function) {
				case "min":
					return min(arguments);

				case "max":
					return max(arguments);

				case "avg":
					return avg(arguments);

				case "abs":
					return Math.abs(single(arguments));

				case "sign":
					return Math.signum(single(arguments));

				case "floor":
					return Math.floor(single(arguments));

				case "ceil":
					return Math.ceil(single(arguments));

				case "round":
					return Math.rint(single(arguments));

				///// power and logarithms
				case "exp":
					return exp(single(arguments));

				case "log":
					return log(single(arguments));

				case "sqrt":
					return sqrt(single(arguments));

				case "pow":
					if (arguments.length != 2) {
						throw new Error("Two argument expected");
					}
					return pow(arguments[0], arguments[1]);

				///// trigonometric functions
				case "sin":
					return sin(single(arguments));

				case "cos":
					return cos(single(arguments));

				case "tan":
					return tan(single(arguments));

				case "sinh":
					return sinh(single(arguments));

				case "cosh":
					return cosh(single(arguments));

				case "tanh":
					return tanh(single(arguments));

				case "asin":
					return asin(single(arguments));

				case "acos":
					return acos(single(arguments));

				case "atan":
					return atan(single(arguments));

				/* todo: untested
				case "asinh":
					return asinh(single(arguments));

				case "acosh":
					return acosh(single(arguments));

				case "atanh":
					return atanh(single(arguments));

				case "sec":
					return sec(single(arguments));

				case "csc":
					return csc(single(arguments));

				case "cot":
					return cot(single(arguments));

				case "sech":
					return sech(single(arguments));

				case "csch":
					return csch(single(arguments));

				case "coth":
					return coth(single(arguments));

				case "asec":
					return asec(single(arguments));

				case "acsc":
					return acsc(single(arguments));

				case "acot":
					return acot(single(arguments));

				case "asech":
					return asech(single(arguments));

				case "acsch":
					return acsch(single(arguments));

				case "acoth":
					return acoth(single(arguments));
				// */
			}
			throw new Error("Invalid function: " + function);
		}

		static final double inf = Double.POSITIVE_INFINITY;
		static final double pi = Math.PI;
		static final double e = Math.E;

		static double single(double[] arguments) throws Error {
			if (arguments.length != 1) {
				throw new Error("Single argument expected");
			}
			return arguments[0];
		}

		static double min(double... arguments) throws Error {
			if (arguments.length < 1) {
				throw new Error("At least one argument expected");
			}
			double value = arguments[0];
			for (int i = 1; i < arguments.length; ++i) {
				if (value > arguments[i]) {
					value = arguments[i];
				}
			}
			return value;
		}
		static double max(double... arguments) throws Error {
			if (arguments.length < 1) {
				throw new Error("At least one argument expected");
			}
			double value = arguments[0];
			for (int i = 1; i < arguments.length; ++i) {
				if (value < arguments[i]) {
					value = arguments[i];
				}
			}
			return value;
		}
		static double avg(double... arguments) {
			double value = 0;
			if (arguments.length < 1) {
				return 0;
			}
			for (double arg : arguments) {
				value += arg;
			}
			return value / arguments.length;
		}

		static double log(double x) { return Math.log(x); }
		static double exp(double x) { return Math.exp(x); }
		static double sqrt(double x) { return Math.sqrt(x); }
		static double pow(double x, double y) { return Math.pow(x, y); }

		/// sine, cosine and tangent: inverse and hyperbolic variants
		static double sin(double x) { return Math.sin(x); }
		static double cos(double x) { return Math.cos(x); }
		static double tan(double x) { return Math.tan(x); /* sin(x) / cos(x); */}

		static double sinh(double x) { return Math.sinh(x); /* (exp(x) - exp(-x)) / 2 */}
		static double cosh(double x) { return Math.cosh(x); /* (exp(x) + exp(-x)) / 2 */}
		static double tanh(double x) { return Math.tanh(x); /* sinh(x) / cosh(x) */}

		static double asin(double x) { return Math.asin(x); /* atan2(x, sqrt((1 + x) * (1 - x))) */}
		static double acos(double x) { return Math.acos(x); /* atan2(sqrt((1 + x) * (1 - x)), x) */}
		static double atan(double x) { return Math.atan(x); /* atan2(x, 1) */}

		static double asinh(double x) { return log(x + sqrt(x * x + 1)); }
		static double acosh(double x) { return log(x + sqrt(x * x - 1)); }
		static double atanh(double x) { return x >= 1 ? inf : x <= -1 ? -inf : .5 * log((1 + x) / (1 - x)); }

		/// secant, cosecant and cotangent: inverse and hyperbolic variants
		static double sec(double x) { return 1 / cos(x); }
		static double csc(double x) { return 1 / sin(x); }
		static double cot(double x) { return 1 / tan(x); }

		static double sech(double x) { return 1 / cosh(x); }
		static double csch(double x) { return 1 / sinh(x); }
		static double coth(double x) { return 1 / tanh(x); }

		static double asec(double x) { return x == 0 ? inf : acos(1 / x); }
		static double acsc(double x) { return x == 0 ? inf : asin(1 / x); }
		static double acot(double x) { return x == 0 ? pi / 2 : atan(1 / x); }

		static double asech(double x) { return x == 0 ? inf : x == 1 ? 0 : log((sqrt(1 - x * x) + 1) / x); }
		static double acsch(double x) { return x == 0 ? inf : x < 0 ? log((1 - sqrt(1 + x * x)) / x) : log((1 + sqrt(1 + x * x)) / x); }
		static double acoth(double x) { return x == 1 ? inf : x == -1 ? -inf : .5 * log((x + 1) / (x - 1)); }
	}
}
