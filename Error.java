/**
 * A general error for parsing and evaluation errors
 */
public class Error extends Exception {

	private Error(String message, Lexer.Token token, int position, String text) {
		super(message + ": `" + text + "`, position: " + position + ", token: " + token);
	}

	public Error(String message, Lexer.Token token, Lexer lexer) {
		this(message, token, lexer.getPosition(), lexer.getText());
	}

	public Error(String message, Parser.Node node) {
		this(message, node.token, node.getPosition(), node.getText());
	}

	public Error(String message, Parser.Node node, Throwable cause) {
		this(message, node.token, node.getPosition(), node.getText());
		initCause(cause);
	}

	public Error(String message) {
		super(message);
	}
}
