/**
 * Visit all nodes in the tree using a visitor.
 */
public interface Visitor {
	/**
	 * Prefix visitor of the node.
	 *
	 * @param node node being visited.
	 */
	void pre(Parser.Node node);

	/**
	 * Postfix visitor of the node.
	 *
	 * @param node node being visited.
	 */
	void post(Parser.Node node);

	/**
	 * Infix visitor of the node.
	 *
	 * @param node node being visited.
	 */
	void visit(Parser.Node node);

	/**
	 * visit each node of the syntax tree.
	 *
	 * @param node    root of the syntax tree.
	 * @param visitor visitor to be used.
	 */
	static void visit(Parser.Node node, Visitor visitor) {
		visitor.pre(node);
		if (node.left != null) {
			visit(node.left, visitor);
		}
		visitor.visit(node);
		if (node.right != null) {
			visit(node.right, visitor);
		}
		visitor.post(node);
	}

	/**
	 * Pretty print the abstract syntax tree.
	 *
	 * @param root        root of the syntax tree.
	 * @param groupArgs   place ( and ) around arguments: `(1, (2, 3))`.
	 * @param groupValues place ( and ) around values and variables.
	 * @return the string representing the abstract syntax tree.
	 */
	static String format(Parser.Node root, boolean groupArgs, boolean groupValues) {
		final StringBuilder out = new StringBuilder();
		Visitor.visit(root, new Visitor() {

			private void group(Parser.Node node, char chr) {
				if (node == root) {
					return;
				}
				if (node.left == null && node.right == null) {
					if (groupValues) {
						out.append(chr);
					}
					return;
				}
				if (node.token == Lexer.Token.Coma) {
					if (groupArgs) {
						out.append(chr);
					}
					return;
				}
				out.append(chr);
			}

			@Override
			public void pre(Parser.Node node) {
				switch (node.token) {
					case Idx: // array [ subscript ]
					case Fun: // function ( arguments )
						// postpone after lhs was printed
						return;
				}
				group(node, '(');
			}

			@Override
			public void post(Parser.Node node) {
				group(node, node.token == Lexer.Token.Idx ? ']' : ')');
			}

			@Override
			public void visit(Parser.Node node) {
				switch (node.token) {
					case Idx:
						// array [ subscript ]
						group(node, '[');
						return;

					case Fun:
						// function ( arguments )
						group(node, '(');
						return;

					case Coma:
						// no spacing before separator: 1, 2, 3, ...
						out.append(node.getText()).append(' ');
						return;
				}

				if (node.left == null && node.right == null) {
					// not an operator, no operands
					out.append(node.getText());
					return;
				}

				if (node.left == null) {
					// no spacing before and after unary operators: ---4
					out.append(node.getText());
					return;
				}

				// spacing before and after binary operators: 3 + 9
				out.append(' ').append(node.getText()).append(' ');
			}
		});
		return out.toString();
	}
}
