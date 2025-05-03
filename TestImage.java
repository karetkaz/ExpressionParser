import java.io.FileWriter;
import java.util.concurrent.TimeUnit;

class TestImage extends Evaluator {

	public static void main(String[] args) throws Exception {
		int width = 1024 / 2;
		int height = width;

		String equationYinYang = "h = x * x + y * y, h > 1 || (" +
				"d = abs(y) - h," +
				"a = d - 0.23," +
				"b = h - 1," +
				"c = sign(a * b * (y + x + (y - x) * sign(d)))," +
				"c = mix(c, 0.0f, smoothstep(1.00f, h, 0.98f))," +
				"c = mix(c, 1.0f, smoothstep(1.02f, h, 1.00f))," +
				"c)";

		long parseTime = System.nanoTime();
		Parser.Node root = Parser.parse(equationYinYang);
		parseTime = System.nanoTime() - parseTime;

		TestImage evaluator = new TestImage();

		// evaluate first time (bytecode will be executed, no jit yet)
		long eval0Time = System.nanoTime();
		evaluator.vars['x'] = 0;
		evaluator.vars['y'] = 0;
		evaluator.evaluate(root);
		eval0Time = System.nanoTime() - eval0Time;

		// write the value of each evaluation to the file
		long execTime = System.nanoTime();
		try (FileWriter img = new FileWriter("test.ppm")) {
			img.append("P2\n")
					.append(String.valueOf(width))
					.append(" ")
					.append(String.valueOf(height))
					.append(" 255\n");

			for (int y = 0; y < height; y += 1) {
				for (int x = 0; x < width; x += 1) {
					evaluator.vars['x'] = 2 * x / (double) width - 1;
					evaluator.vars['y'] = 2 * y / (double) height - 1;
					double value = 256 * evaluator.evaluate(root);
					value = Math.min(Math.max(value, 0), 255);
					if (x > 0) {
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
		evaluator.vars['x'] = 0;
		evaluator.vars['y'] = 0;
		evaluator.evaluate(root);
		eval1Time = System.nanoTime() - eval1Time;

		double unit = TimeUnit.MILLISECONDS.toNanos(1);
		System.out.println("parseTime.millis: " + parseTime / unit);
		System.out.println("eval0Time.millis: " + eval0Time / unit);
		System.out.println("eval1Time.millis: " + eval1Time / unit);
		System.out.println("execTime.millis: " + execTime / unit);
	}

	private final double[] vars;

	public TestImage() {
		this.vars = new double[128];
		for (int i = '0'; i <= '9'; i++) {
			vars[i] = i - '0';
		}
	}

	public int var(Parser.Node node) throws Error {
		if (node.getToken() != Lexer.Token.Value) {
			throw new Error("set can only modify variables", node);
		}
		if (node.getText().length() != 1) {
			throw new Error("variables can be single characters", node);
		}
		char chr = node.getText().charAt(0);
		if (chr >= '0' && chr <= '9') {
			throw new Error("variables can not be numbers", node);
		}
		return chr;
	}

	@Override
	public double onValue(String value) {
		if (value.length() == 1) {
			return vars[value.charAt(0)];
		}
		return Double.parseDouble(value);
	}

	@Override
	protected double onArray(String array, int subscript) throws Error {
		throw new Error("Arrays are not supported");
	}

	@Override
	protected double onFunction(String function, double[] arguments) throws Error {
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
	public double evaluate(Parser.Node node) throws Error {
		switch (node.getToken()) {
			case Set:
				return vars[var(node.getLeft())] = evaluate(node.getRight());
			case SetAdd:
				return vars[var(node.getLeft())] += evaluate(node.getRight());
			case SetSub:
				return vars[var(node.getLeft())] -= evaluate(node.getRight());
			case SetMul:
				return vars[var(node.getLeft())] *= evaluate(node.getRight());
			case SetDiv:
				return vars[var(node.getLeft())] /= evaluate(node.getRight());
			case SetRem:
				return vars[var(node.getLeft())] %= evaluate(node.getRight());

			case Coma:
				// enable chain of expressions, returning the value of the last one
				evaluate(node.getLeft());
				return evaluate(node.getRight());
		}

		return super.evaluate(node);
	}
}
