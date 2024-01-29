/**
 * A general error for parsing and evaluation errors
 */
public class Error extends Exception {

	public Error(String message) {
		super(message);
	}

	public Error(String message, Exception cause) {
		super(message, cause);
	}

	public Error(String message, Parser.Node node) {
		this(message, node.getToken(), node.getPosition(), node.getText());
	}

	public Error(String message, Lexer.Token token, Lexer lexer) {
		this(message, token, lexer.getPosition(), lexer.getText());
	}

	private Error(String message, Lexer.Token token, int position, String text) {
		this(message + ": " + token + "(:" + position + ", '" + text + "')");
	}
}
