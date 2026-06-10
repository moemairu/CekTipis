import java.sql.*;
import java.util.ArrayList;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:cektipis.db";

    private DatabaseManager() {
        try {
            // Explicitly load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    // Initialize database tables
    public void initializeDatabase() {
        String createMenuItems = """
                    CREATE TABLE IF NOT EXISTS menu_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        price REAL NOT NULL,
                        stock INTEGER DEFAULT 0,
                        category TEXT NOT NULL,
                        spicy_level INTEGER DEFAULT 0,
                        is_hot INTEGER DEFAULT 0,
                        has_ice_cream INTEGER DEFAULT 0
                    )
                """;

        String createOrders = """
                    CREATE TABLE IF NOT EXISTS orders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp TEXT NOT NULL,
                        status TEXT DEFAULT 'Pending',
                        payment_method TEXT,
                        payment_amount REAL,
                        total REAL,
                        tax REAL,
                        grand_total REAL
                    )
                """;

        String createOrderItems = """
                    CREATE TABLE IF NOT EXISTS order_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        order_id INTEGER NOT NULL,
                        menu_item_id INTEGER NOT NULL,
                        menu_item_name TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        price REAL NOT NULL,
                        subtotal REAL NOT NULL,
                        FOREIGN KEY (order_id) REFERENCES orders(id),
                        FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
                    )
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createMenuItems);
            stmt.execute(createOrders);
            stmt.execute(createOrderItems);
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    // Seed initial menu items if empty
    public void seedMenuItems() {
        String countQuery = "SELECT COUNT(*) FROM menu_items";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(countQuery)) {

            if (rs.next() && rs.getInt(1) == 0) {
                // Food items
                insertMenuItem("Nasi Goreng", 25000, 50, "Food", 2, 0, 0);
                insertMenuItem("Rendang", 35000, 30, "Food", 3, 0, 0);
                insertMenuItem("Ayam Geprek", 20000, 40, "Food", 5, 0, 0);
                insertMenuItem("Soto Ayam", 18000, 35, "Food", 1, 0, 0);
                insertMenuItem("Mie Goreng", 22000, 45, "Food", 2, 0, 0);

                // Beverages
                insertMenuItem("Es Teh Manis", 5000, 100, "Beverage", 0, 0, 0);
                insertMenuItem("Kopi Hitam", 8000, 80, "Beverage", 0, 1, 0);
                insertMenuItem("Jus Alpukat", 15000, 40, "Beverage", 0, 0, 0);
                insertMenuItem("Teh Hangat", 5000, 100, "Beverage", 0, 1, 0);

                // Desserts
                insertMenuItem("Es Krim", 12000, 50, "Dessert", 0, 0, 1);
                insertMenuItem("Pudding", 10000, 40, "Dessert", 0, 0, 0);
                insertMenuItem("Pisang Goreng", 8000, 60, "Dessert", 0, 0, 0);
            }
        } catch (SQLException e) {
            System.err.println("Error seeding menu items: " + e.getMessage());
        }
    }

    private void insertMenuItem(String name, double price, int stock, String category,
            int spicyLevel, int isHot, int hasIceCream) {
        String sql = "INSERT INTO menu_items (name, price, stock, category, spicy_level, is_hot, has_ice_cream) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setDouble(2, price);
            pstmt.setInt(3, stock);
            pstmt.setString(4, category);
            pstmt.setInt(5, spicyLevel);
            pstmt.setInt(6, isHot);
            pstmt.setInt(7, hasIceCream);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error inserting menu item: " + e.getMessage());
        }
    }

    // Load all menu items from database
    public ArrayList<MenuItem> loadMenuItems() {
        ArrayList<MenuItem> menuItems = new ArrayList<>();
        String sql = "SELECT * FROM menu_items";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int stock = rs.getInt("stock");
                String category = rs.getString("category");

                MenuItem item = null;

                switch (category) {
                    case "Food":
                        int spicyLevel = rs.getInt("spicy_level");
                        item = new Food(name, price, stock, spicyLevel);
                        break;
                    case "Beverage":
                        boolean isHot = rs.getInt("is_hot") == 1;
                        item = new Beverage(name, price, stock, isHot);
                        break;
                    case "Dessert":
                        boolean hasIceCream = rs.getInt("has_ice_cream") == 1;
                        item = new Dessert(name, price, stock, hasIceCream);
                        break;
                }

                if (item != null) {
                    item.setId(id);
                    menuItems.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading menu items: " + e.getMessage());
        }

        return menuItems;
    }

    // Update stock for a menu item
    public void updateStock(int menuItemId, int newStock) {
        String sql = "UPDATE menu_items SET stock = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, newStock);
            pstmt.setInt(2, menuItemId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating stock: " + e.getMessage());
        }
    }

    // Save a completed order
    public int saveOrder(Order order) {
        String orderSql = "INSERT INTO orders (timestamp, status, payment_method, payment_amount, total, tax, grand_total) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, order.getTimestamp().toString());
            pstmt.setString(2, order.getStatus());
            pstmt.setString(3, order.getPaymentMethod());
            pstmt.setDouble(4, order.getPaymentAmount());
            pstmt.setDouble(5, order.getTotal());
            pstmt.setDouble(6, order.getTax());
            pstmt.setDouble(7, order.getGrandTotal());
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                int orderId = keys.getInt(1);
                saveOrderItems(orderId, order);
                return orderId;
            }
        } catch (SQLException e) {
            System.err.println("Error saving order: " + e.getMessage());
        }
        return -1;
    }

    private void saveOrderItems(int orderId, Order order) {
        String sql = "INSERT INTO order_items (order_id, menu_item_id, menu_item_name, quantity, price, subtotal) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Order.OrderItem item : order.getItems()) {
                pstmt.setInt(1, orderId);
                pstmt.setInt(2, item.getMenuItem().getId());
                pstmt.setString(3, item.getMenuItem().getName());
                pstmt.setInt(4, item.getQuantity());
                pstmt.setDouble(5, item.getMenuItem().getPrice());
                pstmt.setDouble(6, item.getSubtotal());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error saving order items: " + e.getMessage());
        }
    }

    // Load completed orders for sales report
    public ArrayList<Order> loadCompletedOrders(ArrayList<MenuItem> menu) {
        ArrayList<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE status = 'Completed'";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int orderId = rs.getInt("id");
                Order order = new Order();
                order.setOrderId(orderId);
                order.setStatus(rs.getString("status"));
                order.setPaymentMethod(rs.getString("payment_method"));
                order.setPaymentAmount(rs.getDouble("payment_amount"));
                // Load order items
                loadOrderItems(order, menu);
                orders.add(order);
            }
        } catch (SQLException e) {
            System.err.println("Error loading orders: " + e.getMessage());
        }
        return orders;
    }

    private void loadOrderItems(Order order, ArrayList<MenuItem> menu) {
        String sql = "SELECT * FROM order_items WHERE order_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, order.getOrderId());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int menuItemId = rs.getInt("menu_item_id");
                int quantity = rs.getInt("quantity");

                // Find menu item by ID
                for (MenuItem item : menu) {
                    if (item.getId() == menuItemId) {
                        order.addItemForLoading(item, quantity);
                        break;
                    }
                }
            }
        } catch (SQLException e) {System.err.println("Error loading order items: " + e.getMessage());}
    }

    // Close connection
    public void close() {
        try {if (connection != null && !connection.isClosed()) {connection.close();}
        } catch (SQLException e) {System.err.println("Error closing connection: " + e.getMessage());}
    }
}
