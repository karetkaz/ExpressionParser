import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

class TestImage extends Evaluator {
	private static final boolean ENABLE_OPTIMIZATION = true;

	public static void main(String[] args) throws IOException {
		int size = 512;

		String equationYinYang = "h = x * x + y * y, h > 1 || ("
				+ "d = abs(y) - h,"
				+ "a = d - 0.23,"
				+ "b = h - 1,"
				+ "c = sign(a * b * (y + x + (y - x) * sign(d))),"
				+ "c = mix(c, 0.0f, smoothstep(1.00f, h, 0.98f)),"
				+ "c = mix(c, 1.0f, smoothstep(1.02f, h, 1.00f)),"
				+ "c)";

		long parseTime = System.nanoTime();
		Parser.Node root = Parser.parse(equationYinYang);
		parseTime = System.nanoTime() - parseTime;

		TestImage evaluator = new TestImage();
		if (ENABLE_OPTIMIZATION) {
			root = evaluator.optimize(root);
		}

		// evaluate first time (bytecode will be executed, no jit yet)
		long eval0Time = System.nanoTime();
		evaluator.x.setValue(0);
		evaluator.y.setValue(0);
		evaluator.evaluate(root);
		eval0Time = System.nanoTime() - eval0Time;

		// write the value of each evaluation to the file
		long execTime = System.nanoTime();
		try (FileWriter img = new FileWriter("test.ppm")) {
			img.append("P2\n")
					.append(String.valueOf(size))
					.append(" ")
					.append(String.valueOf(size))
					.append(" 255\n");

			double center = 2. / (size + 1);
			for (int y = 1; y <= size; y += 1) {
				evaluator.y.setValue(y * center - 1);
				for (int x = 1; x <= size; x += 1) {
					evaluator.x.setValue(x * center - 1);
					double value = 255 * evaluator.evaluate(root);
					value = Math.min(Math.max(value, 0), 255);
					if (x > 1) {
						img.append(" ");
					}
					img.append(String.valueOf((int) value));
				}
				img.append("\n");
			}
		}
		execTime = System.nanoTime() - execTime;

		// evaluate a single time (this time the evaluate function should be jit compiled)
		long eval1Time = System.nanoTime();
		evaluator.x.setValue(0);
		evaluator.y.setValue(0);
		evaluator.evaluate(root);
		eval1Time = System.nanoTime() - eval1Time;

		double unit = TimeUnit.MILLISECONDS.toNanos(1);
		System.out.println("parseTime.millis: " + parseTime / unit);
		System.out.println("eval0Time.millis: " + eval0Time / unit);
		System.out.println("eval1Time.millis: " + eval1Time / unit);
		System.out.println("execTime.millis: " + execTime / unit);
	}

	private final HashMap<String, ToDoubleFunction<double[]>> functions = new HashMap<>();
	private final HashMap<String, VarNode> variables = new HashMap<>();
	private final VarNode x;
	private final VarNode y;

	private TestImage() {
		x = getVarOrPut("x");
		y = getVarOrPut("y");

		functions.put("abs", arguments -> Math.abs(arguments[0]));
		functions.put("sign", arguments -> Math.signum(arguments[0]));
		functions.put("mix", arguments -> {
			double min = arguments[0];
			double max = arguments[1];
			double t = arguments[2];
			return min + t * (max - min);
		});
		functions.put("smoothstep", arguments -> {
			double min = arguments[0];
			double max = arguments[1];
			double t = (arguments[2] - min) / (max - min);
			if (t < 0) {
				return 0;
			}
			if (t > 1) {
				return 1;
			}
			return t * t * (3 - 2 * t);
		});
	}

	@Override
	public double onValue(String value) {
		if (ENABLE_OPTIMIZATION) {
			throw new Error("Not optimized out value: " + value);
		}
		VarNode node = variables.get(value);
		if (node != null) {
			return node.getValue();
		}
		return Double.parseDouble(value);
	}

	@Override
	protected double onArray(String array, int subscript) {
		throw new Error("Arrays are not supported");
	}

	@Override
	protected double onFunction(String function, double[] arguments) {
		if (ENABLE_OPTIMIZATION) {
			throw new Error("Not optimized out function: " + function);
		}
		switch (function) {
			case "abs":
				require(arguments.length == 1, "abs requires one argument");
				return Math.abs(arguments[0]);

			case "sign":
				require(arguments.length == 1, "sign requires one argument");
				return Math.signum(arguments[0]);

			case "mix": {
				require(arguments.length == 3, "Linear interpolation requires three arguments");
				double min = arguments[0];
				double max = arguments[1];
				double t = arguments[2];
				return min + t * (max - min);
			}
			case "smoothstep": {
				require(arguments.length == 3, "Hermite interpolation requires three arguments");
				double min = arguments[0];
				double max = arguments[1];
				double t = (arguments[2] - min) / (max - min);
				if (t < 0) {
					return 0;
				}
				if (t > 1) {
					return 1;
				}
				return t * t * (3 - 2 * t);
			}
		}
		throw new Error("Invalid function: " + function);
	}

	@Override
	public double evaluate(Parser.Node node) {
		if (node instanceof OptimizedNode) {
			return ((OptimizedNode) node).getValue();
		}

		switch (node.getToken()) {
			case Set:
				require(!ENABLE_OPTIMIZATION, "Not optimized out assignment");
				// set or create a new variable and update its value
				return getVarOrPut(node.getLeft().getText()).setValue(evaluate(node.getRight()));

			case Coma:
				// enable a chain of expressions, returning the value of the last one
				evaluate(node.getLeft());
				return evaluate(node.getRight());
		}

		return super.evaluate(node);
	}

	@Override
	protected Parser.Node optimize(Parser.Node node) {
		if (node == null) {
			return null;
		}

		switch (node.getToken()) {
			case Value:
				// replace with a value or variabl node
				try {
					VarNode var = variables.get(node.getText());
					if (var != null) {
						return var;
					}
					double value = Double.parseDouble(node.getText());
					return new VarNode(node, value);
				} catch (NumberFormatException e) {
					throw new Error("Invalid value or variable", node, e);
				}

			case Fun:
				if (node.getLeft() == null) {
					// replace node with subtree: (3 + 7)
					return optimize(node.getRight());
				}
				// replace node with function: min(5)
				require(node.getLeft().getToken() == Lexer.Token.Value, "must be a value");
				return new FunNode(functions.get(node.getLeft().getText()), optimize(node.getRight()));

			case Set:
				// replace or create variable: x = 5
				require(node.getLeft().getToken() == Lexer.Token.Value, "must be a value");
				return new SetNode(getVarOrPut(node.getLeft().getText()), optimize(node.getRight()));
		}

		return super.optimize(node);
	}

	private VarNode getVarOrPut(String name) {
		return variables.computeIfAbsent(name, VarNode::new);
	}

	interface OptimizedNode {
		double getValue();
	}

	// node representing a parsed value or variable, like: `3.1415...`
	private static class VarNode extends Parser.Node implements OptimizedNode {
		private double value;

		protected VarNode(String text) {
			super(Lexer.Token.Value, -1, text);
		}

		protected VarNode(Parser.Node node, double value) {
			super(node.getToken(), node.getPosition(), node.getText());
			this.value = value;
		}

		@Override
		public double getValue() {
			return value;
		}

		public double setValue(double value) {
			return this.value = value;
		}

		@Override
		public String toString() {
			return token + "(" + getText() + ": " + value + ")";
		}
	}

	// node representing a variable update, like: `x = 3 * y`
	private class SetNode extends Parser.Node implements OptimizedNode {
		private final VarNode var;

		protected SetNode(VarNode var, Parser.Node value) {
			super(Lexer.Token.Set, -1, "<optimized variable set node>");
			this.right = value;
			this.var = var;
		}

		@Override
		public double getValue() {
			return var.setValue(TestImage.this.evaluate(this.right));
		}
	}

	// node representing a function call, like: `smoothstep(1, h, .98)`
	private class FunNode extends Parser.Node implements OptimizedNode {
		private final double[] arguments;
		private final ToDoubleFunction<double[]> fun;

		protected FunNode(ToDoubleFunction<double[]> fun, Parser.Node args) {
			super(Lexer.Token.Fun, -1, "<optimized function call node>");
			this.arguments = new double[Evaluator.countArgs(args)];
			this.right = args;
			this.fun = fun;
		}

		@Override
		public double getValue() {
			TestImage.this.evaluateArguments(arguments, 0, this.right);
			return fun.applyAsDouble(arguments);
		}
	}
}
