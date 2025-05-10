import java.sql.*;
import java.math.BigDecimal;
import java.util.Scanner;

public class MiniJavaStoreDB {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/MiniJavaStoreDB";
    private static final String USER = "root"; // change if needed
    private static final String PASS = "";     // change if needed

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            initializeDatabase(); // Initialize DB and tables

            try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
                while (true) {
                    System.out.println("\n=== Mini Java Store ===");
                    System.out.println("[1] View Items");
                    System.out.println("[2] Buy Item (Add to Cart)");
                    System.out.println("[3] Remove Item from Cart");
                    System.out.println("[4] View Cart");
                    System.out.println("[5] Checkout (Pay)");
                    System.out.println("[6] Clear Cart");
                    System.out.println("[7] Add New Item");
                    System.out.println("[8] Update Item");
                    System.out.println("[9] Delete Item");
                    System.out.println("[10] View Inventory");
                    System.out.println("[0] Exit");
                    System.out.print("Choose an option 0-10: ");
                    String choice = scanner.nextLine();

                    switch (choice) {
                        case "1": viewItems(conn); break;
                        case "2": buyItem(conn); break;
                        case "3": removeItemFromCart(conn); break;
                        case "4": viewCart(conn); break;
                        case "5": checkout(conn); break;
                        case "6": clearCart(conn); break;
                        case "7": addItem(conn); break;
                        case "8": updateItem(conn); break;
                        case "9": deleteItem(conn); break;
                        case "10": viewInventory(conn); break;
                        case "0": System.out.println("Exiting..."); return;
                        default: System.out.println("Invalid choice.");
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void initializeDatabase() {
        String baseUrl = "jdbc:mysql://localhost:3306/";
        try (Connection conn = DriverManager.getConnection(baseUrl, USER, PASS);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS MiniJavaStoreDB");

            try (Connection dbConn = DriverManager.getConnection(DB_URL, USER, PASS);
                 Statement dbStmt = dbConn.createStatement()) {

                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS items (" +
                        "code VARCHAR(10) PRIMARY KEY," +
                        "name VARCHAR(100)," +
                        "price DECIMAL(10,2)," +
                        "stocks INT)");

                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS cart (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "code VARCHAR(10)," +
                        "quantity INT," +
                        "total_price DECIMAL(10,2))");

                ResultSet rs = dbStmt.executeQuery("SELECT COUNT(*) FROM items");
                if (rs.next() && rs.getInt(1) == 0) {
                    dbStmt.executeUpdate("INSERT INTO items (code, name, price, stocks) VALUES " +
                            "('A001', 'Milk', 50.00, 20)," +
                            "('B002', 'Bread', 30.00, 15)," +
                            "('C003', 'Eggs', 10.00, 50)");
                    System.out.println("Initial data populated.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void viewItems(Connection conn) throws SQLException {
        String sql = "SELECT code, name, price FROM items";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\nAvailable Items:");
            System.out.printf("%-10s %-30s %-10s\n", "Code", "Description", "Price");
            while (rs.next()) {
                System.out.printf("%-10s %-30s P%-10.2f\n",
                        rs.getString("code"), rs.getString("name"), rs.getBigDecimal("price"));
            }
        }
    }

    private static void buyItem(Connection conn) throws SQLException {
        System.out.print("Enter item code: ");
        String code = scanner.nextLine().trim().toUpperCase();
        if (code.isEmpty()) return;

        String sql = "SELECT * FROM items WHERE code = ?";
        conn.setAutoCommit(false); // begin transaction

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("Item not found.");
                conn.rollback();
                return;
            }

            String name = rs.getString("name");
            BigDecimal price = rs.getBigDecimal("price");
            int stocks = rs.getInt("stocks");

            System.out.print("Enter quantity: ");
            int qty;
            try {
                qty = Integer.parseInt(scanner.nextLine());
                if (qty <= 0 || qty > stocks) {
                    System.out.println("Invalid or insufficient quantity.");
                    conn.rollback();
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number.");
                conn.rollback();
                return;
            }

            BigDecimal totalPrice = price.multiply(BigDecimal.valueOf(qty));
            int newStocks = stocks - qty;

            try (PreparedStatement updatePS = conn.prepareStatement("UPDATE items SET stocks = ? WHERE code = ?")) {
                updatePS.setInt(1, newStocks);
                updatePS.setString(2, code);
                updatePS.executeUpdate();
            }

            try (PreparedStatement checkPS = conn.prepareStatement("SELECT * FROM cart WHERE code = ?")) {
                checkPS.setString(1, code);
                ResultSet cartRS = checkPS.executeQuery();

                if (cartRS.next()) {
                    int newQty = cartRS.getInt("quantity") + qty;
                    BigDecimal newTotal = cartRS.getBigDecimal("total_price").add(totalPrice);
                    try (PreparedStatement updateCart = conn.prepareStatement(
                            "UPDATE cart SET quantity = ?, total_price = ? WHERE code = ?")) {
                        updateCart.setInt(1, newQty);
                        updateCart.setBigDecimal(2, newTotal);
                        updateCart.setString(3, code);
                        updateCart.executeUpdate();
                    }
                } else {
                    try (PreparedStatement insertPS = conn.prepareStatement(
                            "INSERT INTO cart (code, quantity, total_price) VALUES (?, ?, ?)")) {
                        insertPS.setString(1, code);
                        insertPS.setInt(2, qty);
                        insertPS.setBigDecimal(3, totalPrice);
                        insertPS.executeUpdate();
                    }
                }
            }

            conn.commit();
            System.out.println("Added to cart: " + name + " x" + qty);
        } catch (SQLException e) {
            conn.rollback();
            e.printStackTrace();
        } finally {
            conn.setAutoCommit(true); // reset auto-commit
        }
    }

    private static void viewCart(Connection conn) throws SQLException {
        String sql = "SELECT c.code, i.name, c.quantity, c.total_price FROM cart c JOIN items i ON c.code = i.code";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            BigDecimal total = BigDecimal.ZERO;
            System.out.println("\nYour Cart:");
            System.out.printf("%-10s %-30s %-10s %-10s\n", "Code", "Item", "Qty", "Price");
            while (rs.next()) {
                System.out.printf("%-10s %-30s %-10d P%-10.2f\n",
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getBigDecimal("total_price"));
                total = total.add(rs.getBigDecimal("total_price"));
            }
            System.out.println("Total: P" + total.setScale(2));
        }
    }

    private static void checkout(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        try {
            viewCart(conn);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM cart");
            }
            conn.commit();
            System.out.println("Checked out successfully.");
        } catch (Exception e) {
            conn.rollback();
            System.out.println("Checkout failed.");
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void clearCart(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sql = "SELECT code, quantity FROM cart";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    int qty = rs.getInt("quantity");
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE items SET stocks = stocks + ? WHERE code = ?")) {
                        ps.setInt(1, qty);
                        ps.setString(2, code);
                        ps.executeUpdate();
                    }
                }
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM cart");
            }

            conn.commit();
            System.out.println("Cart has been cleared and items restocked.");
        } catch (Exception e) {
            conn.rollback();
            System.out.println("Failed to clear cart.");
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void removeItemFromCart(Connection conn) throws SQLException {
        System.out.print("Enter item code to remove from cart: ");
        String code = scanner.nextLine().trim().toUpperCase();

        conn.setAutoCommit(false);
        try (PreparedStatement selectStmt = conn.prepareStatement("SELECT quantity FROM cart WHERE code = ?")) {
            selectStmt.setString(1, code);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                int qty = rs.getInt("quantity");

                try (PreparedStatement updateInventory = conn.prepareStatement("UPDATE items SET stocks = stocks + ? WHERE code = ?")) {
                    updateInventory.setInt(1, qty);
                    updateInventory.setString(2, code);
                    updateInventory.executeUpdate();
                }

                try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM cart WHERE code = ?")) {
                    deleteStmt.setString(1, code);
                    deleteStmt.executeUpdate();
                }

                conn.commit();
                System.out.println("Item removed from cart and restocked.");
            } else {
                conn.rollback();
                System.out.println("Item not found in cart.");
            }
        } catch (Exception e) {
            conn.rollback();
            e.printStackTrace();
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void addItem(Connection conn) throws SQLException {
        System.out.print("Enter code: ");
        String code = scanner.nextLine().trim().toUpperCase();
        String checkSql = "SELECT * FROM items WHERE code = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, code);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("Item code already exists!");
                return;
            }
        }

        System.out.print("Enter name: ");
        String name = scanner.nextLine().trim();

        try {
            System.out.print("Enter price: ");
            BigDecimal price = new BigDecimal(scanner.nextLine());
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                System.out.println("Price cannot be negative.");
                return;
            }

            System.out.print("Enter stocks: ");
            int stocks = Integer.parseInt(scanner.nextLine());
            if (stocks < 0) {
                System.out.println("Stocks cannot be negative.");
                return;
            }

            String sql = "INSERT INTO items (code, name, price, stocks) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, code);
                ps.setString(2, name);
                ps.setBigDecimal(3, price);
                ps.setInt(4, stocks);
                ps.executeUpdate();
                System.out.println("Item added.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid number input.");
        }
    }

    private static void updateItem(Connection conn) throws SQLException {
        System.out.print("Enter item code to update: ");
        String code = scanner.nextLine().trim().toUpperCase();

        String selectSql = "SELECT * FROM items WHERE code = ?";
        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, code);
            ResultSet rs = selectStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Item not found.");
                return;
            }

            String name = rs.getString("name");
            BigDecimal price = rs.getBigDecimal("price");
            int stocks = rs.getInt("stocks");

            System.out.println("Current name: " + name);
            System.out.print("New name (or press Enter to keep): ");
            String newName = scanner.nextLine();
            if (!newName.isBlank()) name = newName;

            try {
                System.out.println("Current price: " + price);
                System.out.print("New price (or press Enter to keep): ");
                String newPriceInput = scanner.nextLine();
                if (!newPriceInput.isBlank()) price = new BigDecimal(newPriceInput);
                if (price.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println("Price cannot be negative.");
                    return;
                }

                System.out.println("Current stocks: " + stocks);
                System.out.print("New stocks (or press Enter to keep): ");
                String newStockInput = scanner.nextLine();
                if (!newStockInput.isBlank()) stocks = Integer.parseInt(newStockInput);
                if (stocks < 0) {
                    System.out.println("Stocks cannot be negative.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number input.");
                return;
            }

            String updateSql = "UPDATE items SET name = ?, price = ?, stocks = ? WHERE code = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, name);
                updateStmt.setBigDecimal(2, price);
                updateStmt.setInt(3, stocks);
                updateStmt.setString(4, code);
                updateStmt.executeUpdate();
                System.out.println("Item updated.");
            }
        }
    }

    private static void deleteItem(Connection conn) throws SQLException {
        System.out.print("Enter item code to delete: ");
        String code = scanner.nextLine().trim().toUpperCase();

        try (PreparedStatement deleteCart = conn.prepareStatement("DELETE FROM cart WHERE code = ?")) {
            deleteCart.setString(1, code);
            deleteCart.executeUpdate();
        }

        try (PreparedStatement deleteItem = conn.prepareStatement("DELETE FROM items WHERE code = ?")) {
            deleteItem.setString(1, code);
            int rows = deleteItem.executeUpdate();
            if (rows > 0) {
                System.out.println("Item deleted.");
            } else {
                System.out.println("Item code not found.");
            }
        }
    }

    private static void viewInventory(Connection conn) throws SQLException {
        String sql = "SELECT code, name, stocks FROM items";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\nInventory:");
            System.out.printf("%-10s %-30s %-10s\n", "Code", "Name", "Stocks");
            while (rs.next()) {
                System.out.printf("%-10s %-30s %-10d\n",
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("stocks"));
            }
        }
    }
}
