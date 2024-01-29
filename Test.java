public class Test {
	private static final double epsilon = 0;
	private static final double epsilonTrig = 1e-15;

	static final double x = Math.random() * 100;
	static final double y = x + Math.random() * 100;
	static final double z = y + Math.random() * 100;
	static final double w = Math.random() * -100;
	static final double[] vec = {x, y, z, w};

	public static void main(String[] args) throws Exception {
		testPow();
		testValues();
		testBitwise();
		testArithmetic();
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

		//* Errors
		assertEquals(x, "(0 : 50) ? x : y", epsilon, false, null);
		assertEquals(x, "(0 ? 50) ? x : y", epsilon, false, null);

		assertErrors("~3.14", epsilon, false, "3.14");
		assertErrors("3.14 & 1", epsilon, false, "3.14 1");

		assertErrors("x(x())", epsilon, false, "x(#0)x(#1)");
		assertErrors("x[x[]]", epsilon, false, "");
		assertErrors("vec()", epsilon, false, "vec(#0)");
		assertErrors("vec[]", epsilon, false, "");
		assertEquals(x, "(x)", epsilon, false, "x");
		assertEquals(x, "[x]", epsilon, false, "");
		assertErrors("(vec)", epsilon, false, "");
		assertErrors("[vec]", epsilon, false, "");
		assertErrors("()", epsilon, false, "");
		assertErrors("[]", epsilon, false, "");
		assertErrors("(([([])]))", epsilon, false, "");
		assertErrors("(([([])]))", epsilon, false, "");
		// */
	}

	public static void testValues() throws Exception {
		assertEquals(4, "4", epsilon, false, "4");
		assertEquals(4, "4.", epsilon, false, "4.");
		assertEquals(4, "4.000", epsilon, false, "4.000");
		assertEquals(4, "(4)", epsilon, false, "4");
		assertEquals(Math.E, "e", epsilon, false, "e");
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
		assertEquals(8, "3 ----5", epsilon, false, "3 5 - - - -");
		assertEquals(8, "--3 +--5", epsilon, false, "3 - - 5 - - +");
		assertEquals(3, "3 ----5 - 5", epsilon, false, "3 5 - - - - 5 -");

		// ',' associates right to left
		assertEquals(1, "min(1, 2, 3)", epsilon, true, "1 2 3 min(#3)");

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
		assertEquals(0, "abs(0)", epsilon, false, "0 abs(#1)");
		assertEquals(1, "abs(1)", epsilon, false, "1 abs(#1)");
		assertEquals(1, "abs(-1)", epsilon, false, "1 - abs(#1)");
		assertEquals(0, "sin(0)", epsilon, false, "0 sin(#1)");
		assertEquals(1, "cos(0)", epsilon, false, "0 cos(#1)");
		assertEquals(0, "sin(pi)", epsilonTrig, false, "pi sin(#1)");
		assertEquals(-1, "cos(pi)", epsilon, false, "pi cos(#1)");
		assertEquals(1, "sin(pi / 2)", epsilon, false, "pi 2 / sin(#1)");
		assertEquals(0, "cos(pi / 2)", epsilonTrig, false, "pi 2 / cos(#1)");
		assertEquals(-1, "sin(3 * pi / 2)", epsilon, false, "3 pi * 2 / sin(#1)");
		assertEquals(0, "cos(3 * pi / 2)", epsilonTrig, false, "3 pi * 2 / cos(#1)");
		assertEquals(.4, "min(1., .9, 4., .4, .8)", epsilon, false, "1. .9 4. .4 .8 min(#5)");
		assertEquals(4., "max(1., .9, 4., .4, .8)", epsilon, false, "1. .9 4. .4 .8 max(#5)");
		assertEquals(22.8, "avg(100., 2., 3., 4., 5)", epsilon, false, "100. 2. 3. 4. 5 avg(#5)");
		assertEquals(.3, "min(.8, max(1., .9, 4., .4, .8), .4, max(.1, .3, .2), .4, max(1., .9, 4., .4, .8))", epsilon, false, ".8 1. .9 4. .4 .8 max(#5) .4 .1 .3 .2 max(#3) .4 1. .9 4. .4 .8 max(#5) min(#6)");

		assertEquals(x, "vec[0]", epsilon, false, "vec[0]");
		assertEquals(y, "vec[1]", epsilon, false, "vec[1]");
		assertEquals(z, "vec[2]", epsilon, false, "vec[2]");
		assertEquals(w, "vec[3]", epsilon, false, "vec[3]");
		assertErrors("vec[4]", epsilon, false, "vec[4]");

		assertEquals(x, "vec[x]", epsilon, false, "vec[x]");
		assertEquals(y, "vec[y]", epsilon, false, "vec[y]");
		assertEquals(z, "vec[z]", epsilon, false, "vec[z]");
		assertEquals(w, "vec[w]", epsilon, false, "vec[w]");
		assertErrors("vec[a]", epsilon, false, "vec[a]");

		assertEquals(w, "min(vec[0], vec[1], vec[2], vec[3])", epsilon, false, "vec[0] vec[1] vec[2] vec[3] min(#4)");
		assertEquals(x, "min(vec[0], vec[1], vec[2])", epsilon, false, "vec[0] vec[1] vec[2] min(#3)");
		assertEquals(x, "min(vec[0], vec[1])", epsilon, false, "vec[0] vec[1] min(#2)");
		assertEquals(x, "min(vec[0])", epsilon, false, "vec[0] min(#1)");

		assertEquals(w, "min(vec[x], vec[y], vec[z], vec[w])", epsilon, false, "vec[x] vec[y] vec[z] vec[w] min(#4)");
		assertEquals(x, "min(vec[x], vec[y], vec[z])", epsilon, false, "vec[x] vec[y] vec[z] min(#3)");
		assertEquals(x, "min(vec[x], vec[y])", epsilon, false, "vec[x] vec[y] min(#2)");
		assertEquals(x, "min(vec[x])", epsilon, false, "vec[x] min(#1)");

		assertEquals(w, "min(vec)", epsilon, false, "vec min(#1)");
		assertErrors("min()", epsilon, false, "min(#0)");
	}

	public static void testPow() throws Exception {
		if (!Lexer.Token.Pow.right2Left) {
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

		// assertEquals(-4, "-2 ** 2", epsilon, false, "2 2 ** -");
		assertEquals(-4, "-(2 ** 2)", epsilon, false, "2 2 ** -");
		// assertEquals(-.25, "-2 ** -2", epsilon, false, "2 2 - ** -");
		assertEquals(-.25, "-(2 ** -2)", epsilon, false, "2 2 - ** -");
	}

	private static void assertErrors(String expression, double epsilon, boolean group, String rpn) throws Error {
		assertEquals(Double.NaN, expression, epsilon, group, rpn);
	}

	private static void assertEquals(double expected, String expression, double epsilon, boolean group, String rpn) throws Error {
		Parser.Node root = Parser.parse(expression);

		// Evaluator which appends to the out buffer the evaluated values (reverse polish notation)
		// except for short-circuiting operators, where only the evaluated values are printed
		// and also allows invalid structures like: `[]`, `()`, `[x]`, `x[]`, ...
		Evaluator evaluator = new Evaluator() {
			final StringBuilder out = new StringBuilder();

			@Override
			public double onFunction(String function, double[] arguments) throws Error {
				out.append(function).append("(#").append(arguments.length).append(")");
				return Test.evaluator.onFunction(function, arguments);
			}

			@Override
			public double onFunction(String function, String argument) throws Error {
				out.append(argument).append(' ').append(function).append("(#1)");
				return Test.evaluator.onFunction(function, argument);
			}

			@Override
			public double onIndex(String array, int subscript) throws Error {
				out.append(array).append("[").append(subscript).append("]");
				return Test.evaluator.onIndex(array, subscript);
			}

			@Override
			public double onIndex(String object, String property) throws Error {
				out.append(object).append("[").append(property).append("]");
				return Test.evaluator.onIndex(object, property);
			}

			@Override
			public double onValue(String text) throws Error {
				return Test.evaluator.onValue(text);
			}

			@Override
			public double evaluate(Parser.Node node) {
				try {
					if (node.getToken() == Lexer.Token.Fun || node.getToken() == Lexer.Token.Idx) {
						if (node.getLeft() == null && node.getRight() == null) {
							return Double.NaN; // [] || ()
						}

						if (node.getRight() == null && node.getToken() == Lexer.Token.Idx) {
							return super.evaluate(node.getLeft()); // x[]
						}

						if (node.getLeft() == null && node.getToken() == Lexer.Token.Idx) {
							return super.evaluate(node.getRight()); // [x]
						}

						double value = super.evaluate(node);
						if (node.getLeft() != null && node != root) {
							out.append(' ');
						}
						return value;
					}

					if (group && node != root) {
						if (node.getLeft() != null || node.getRight() != null) {
							out.append('(');
						}
					}
					double value = super.evaluate(node);
					out.append(node.getText());
					if (group && node != root) {
						if (node.getLeft() != null || node.getRight() != null) {
							out.append(')');
						}
					}
					if (node != root) {
						out.append(' ');
					}
					return value;
				} catch (Exception e) {
					System.err.println(e);
					return Double.NaN;
				}
			}

			@Override
			public String toString() {
				return out.toString().trim();
			}
		};

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
		// System.out.println("`" + expression + "` := `" + Visitor.format(root, true, false) + "` -> `" + evaluation + "` := " + value);
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

	static final Evaluator evaluator = new Evaluator() {
		@Override
		public double onFunction(String function, String argument) throws Error {
			if (argument.equals("vec")) {
				// treat `vec` as an array: min(vec), max(vec), ...
				return onFunction(function, vec);
			}
			return super.onFunction(function, argument);
		}

		@Override
		public double onFunction(String function, double[] arguments) throws Error {
			if ("sign".equalsIgnoreCase(function)) {
				if (arguments.length != 1) {
					throw new Error("Function requires one argument: " + function);
				}
				return Math.signum(arguments[0]);
			}
			if ("abs".equalsIgnoreCase(function)) {
				if (arguments.length != 1) {
					throw new Error("Function requires one argument: " + function);
				}
				return Math.abs(arguments[0]);
			}

			if ("min".equalsIgnoreCase(function)) {
				if (arguments.length < 1) {
					throw new Error("Function requires at least one argument: " + function);
					// or return NaN
				}
				double value = arguments[0];
				for (int i = 1; i < arguments.length; ++i) {
					if (value > arguments[i]) {
						value = arguments[i];
					}
				}
				return value;
			}
			if ("max".equalsIgnoreCase(function)) {
				if (arguments.length < 1) {
					throw new Error("Function requires at least one argument: " + function);
					// or return NaN
				}
				double value = arguments[0];
				for (int i = 1; i < arguments.length; ++i) {
					if (value < arguments[i]) {
						value = arguments[i];
					}
				}
				return value;
			}
			if ("avg".equalsIgnoreCase(function)) {
				double value = 0;
				if (arguments.length < 1) {
					return 0;
				}
				for (double arg : arguments) {
					value += arg;
				}
				return value / arguments.length;
			}

			if ("pow".equalsIgnoreCase(function)) {
				if (arguments.length != 2) {
					throw new Error("Function requires two arguments: " + function);
				}
				return Math.pow(arguments[0], arguments[1]);
			}
			if ("sqrt".equalsIgnoreCase(function)) {
				if (arguments.length != 1) {
					throw new Error("Function requires one argument: " + function);
				}
				return Math.sqrt(arguments[0]);
			}
			if ("mix".equalsIgnoreCase(function)) {
				if (arguments.length != 3) {
					throw new Error("Function requires three arguments: " + function);
				}
				double min = arguments[0];
				double max = arguments[1];
				double t = arguments[2];
				return min + t * (max - min);
			}
			if ("smoothstep".equalsIgnoreCase(function)) {
				// Returns the Hermite interpolation between two values
				if (arguments.length != 3) {
					throw new Error("Function requires three arguments: " + function);
				}
				double min = arguments[0];
				double max = arguments[1];
				double t = arguments[2];
				t = (t - min) / (max - min);
				if (t < 0) {
					t = 0;
				}
				if (t > 1) {
					t = 1;
				}
				return t * t * (3 - 2 * t);
			}

			if ("sin".equalsIgnoreCase(function)) {
				if (arguments.length != 1) {
					throw new Error("Function requires one argument: " + function);
				}
				return Math.sin(arguments[0]);
			}
			if ("cos".equalsIgnoreCase(function)) {
				if (arguments.length != 1) {
					throw new Error("Function requires one argument: " + function);
				}
				return Math.cos(arguments[0]);
			}
			throw new Error("Undefined function: " + function);
		}

		@Override
		public double onIndex(String array, int subscript) throws Error {
			if ("vec".equals(array)) {
				// enable vec[0], vec[1], vec[2], vec[3], ...
				return vec[subscript];
			}
			throw new Error("Undefined array: " + array);
		}

		@Override
		public double onIndex(String object, String property) throws Error {
			if ("vec".equals(object)) {
				// enable vec[x], vec[y], vec[z], vec[w]
				switch (property) {
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
			return super.onIndex(object, property);
		}

		@Override
		public double onValue(String value) throws Error {
			if ("pi".equalsIgnoreCase(value)) {
				return Math.PI;
			}
			if ("e".equalsIgnoreCase(value)) {
				return Math.E;
			}
			if ("x".equalsIgnoreCase(value)) {
				return x;
			}
			if ("y".equalsIgnoreCase(value)) {
				return y;
			}
			if ("z".equalsIgnoreCase(value)) {
				return z;
			}
			if ("w".equalsIgnoreCase(value)) {
				return w;
			}
			return super.onValue(value);
		}
	};
}
