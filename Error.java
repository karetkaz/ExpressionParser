/**
 * Represents a custom exception class for errors during parsing and evaluation.
 */
public class Error extends Exception {

	/**
	 * Constructs a new Error instance with specified details.
	 *
	 * @param message A descriptive error message indicating the nature of the error.
	 * @param token The token associated with the error, providing contextual information.
	 * @param position The position in the input where the error occurred.
	 * @param text The text content related to the token at the error position.
	 */
	private Error(String message, Lexer.Token token, int position, String text) {
		super(message + ": Token." + token + "(`" + text + "`), at position: " + position);
	}

	/**
	 * Constructs a new Error instance with specified details derived from the lexer.
	 *
	 * @param message A descriptive error message indicating the nature of the error.
	 * @param token The token associated with the error, providing contextual information.
	 * @param lexer The lexer instance providing the position and text content related to the error.
	 */
	public Error(String message, Lexer.Token token, Lexer lexer) {
		this(message, token, lexer.getPosition(), lexer.getText());
	}

	/**
	 * Constructs a new Error instance with specified details derived from a parser node.
	 *
	 * @param message A descriptive error message indicating the nature of the error.
	 * @param node The parser node associated with the error, providing the token, position, and text related to the error context.
	 */
	public Error(String message, Parser.Node node) {
		this(message, node.token, node.getPosition(), node.getText());
	}

	/**
	 * Constructs a new Error instance with specified details derived from a parser node
	 * and an additional underlying cause.
	 *
	 * @param message A descriptive error message indicating the nature of the error.
	 * @param node The parser node associated with the error, providing the token, position, and text related to the error context.
	 * @param cause The underlying cause of this error, represented as a Throwable.
	 */
	public Error(String message, Parser.Node node, Throwable cause) {
		this(message, node.token, node.getPosition(), node.getText());
		initCause(cause);
	}

	/**
	 * Constructs a new Error instance with a specified error message.
	 *
	 * @param message A descriptive error message indicating the nature of the error.
	 */
	public Error(String message) {
		super(message);
	}
}
