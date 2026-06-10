import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class POSRestaurant extends JFrame {
    private ArrayList<MenuItem> menu;
    private Order currentOrder;
    private ArrayList<Order> completedOrders;
    private DatabaseManager dbManager;

    // GUI Components
    private JTabbedPane tabbedPane;
    private DefaultTableModel cartTableModel;
    private JTable cartTable;
    private JLabel totalLabel, taxLabel, grandTotalLabel;
    private JTextField paymentField;
    private JComboBox<String> paymentMethodCombo;
    private DefaultListModel<MenuItem> menuListModel;

    public POSRestaurant() {
        setTitle("🍽️ Warung Padang POS System");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // Initialize database
        dbManager = DatabaseManager.getInstance();
        dbManager.initializeDatabase();
        dbManager.seedMenuItems();
        // Initialize data from database
        menu = dbManager.loadMenuItems();
        currentOrder = new Order();
        completedOrders = dbManager.loadCompletedOrders(menu);
        // Create GUI
        createGUI();
        // Add shutdown hook to close database
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dbManager.close();
            }
        });
        setVisible(true);
    }

    private void createGUI() {
        tabbedPane = new JTabbedPane();
        // Tab 1: Cashier
        tabbedPane.addTab("💰 Cashier", createCashierPanel());
        // Tab 2: Stock Management
        tabbedPane.addTab("📦 Stock", createStockPanel());
        // Tab 3: Sales Report
        tabbedPane.addTab("📊 Sales", createSalesPanel());
        add(tabbedPane);
    }

    private JPanel createCashierPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left: Menu
        JPanel menuPanel = new JPanel(new BorderLayout());
        menuPanel.setBorder(BorderFactory.createTitledBorder("Menu Items"));

        menuListModel = new DefaultListModel<>();
        updateMenuList();

        JList<MenuItem> menuList = new JList<>(menuListModel);
        menuList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MenuItem) {
                    MenuItem item = (MenuItem) value;
                    setText(item.getDescription() + " | Stock: " + item.getStock());
                    if (item.getStock() == 0) {
                        setForeground(Color.RED);
                    }
                }
                return this;
            }
        });

        JScrollPane menuScroll = new JScrollPane(menuList);
        menuPanel.add(menuScroll, BorderLayout.CENTER);
        JButton addButton = new JButton("Add to Cart");
        addButton.addActionListener(e -> {
            MenuItem selected = menuList.getSelectedValue();
            if (selected != null) {
                addToCart(selected);
            }
        });
        menuPanel.add(addButton, BorderLayout.SOUTH);

        // Right: Cart & Payment
        JPanel cartPanel = new JPanel(new BorderLayout());
        cartPanel.setBorder(BorderFactory.createTitledBorder("Shopping Cart"));
        // Cart Table
        String[] columns = { "Item", "Price", "Qty", "Subtotal" };
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only quantity editable
            }
        };
        cartTable = new JTable(cartTableModel);
        cartTable.getModel().addTableModelListener(e -> {
            if (e.getColumn() == 2) { // Quantity changed
                updateCartQuantity(e.getFirstRow());
            }
        });
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartPanel.add(cartScroll, BorderLayout.CENTER);

        // Cart buttons
        JPanel cartButtons = new JPanel(new FlowLayout());
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(e -> removeFromCart());
        JButton clearButton = new JButton("Clear Cart");
        clearButton.addActionListener(e -> clearCart());
        cartButtons.add(removeButton);
        cartButtons.add(clearButton);
        cartPanel.add(cartButtons, BorderLayout.NORTH);

        // Payment Panel
        JPanel paymentPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        paymentPanel.setBorder(BorderFactory.createTitledBorder("Payment"));

        totalLabel = new JLabel("Rp 0");
        taxLabel = new JLabel("Rp 0");
        grandTotalLabel = new JLabel("Rp 0");
        grandTotalLabel.setFont(new Font("Arial", Font.BOLD, 16));

        paymentMethodCombo = new JComboBox<>(new String[] { "Cash", "Debit Card", "E-Wallet" });
        paymentField = new JTextField();

        paymentPanel.add(new JLabel("Subtotal:"));
        paymentPanel.add(totalLabel);
        paymentPanel.add(new JLabel("Tax (10%):"));
        paymentPanel.add(taxLabel);
        paymentPanel.add(new JLabel("GRAND TOTAL:"));
        paymentPanel.add(grandTotalLabel);
        paymentPanel.add(new JLabel("Payment Method:"));
        paymentPanel.add(paymentMethodCombo);
        paymentPanel.add(new JLabel("Payment Amount:"));
        paymentPanel.add(paymentField);

        JButton payButton = new JButton("💳 Process Payment");
        payButton.setFont(new Font("Arial", Font.BOLD, 14));
        payButton.setBackground(new Color(255, 140, 0));
        payButton.setForeground(Color.WHITE);
        payButton.addActionListener(e -> processPayment());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(paymentPanel, BorderLayout.CENTER);
        bottomPanel.add(payButton, BorderLayout.SOUTH);

        cartPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Split layout
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, menuPanel, cartPanel);
        splitPane.setDividerLocation(500);
        panel.add(splitPane);

        return panel;
    }

    private JPanel createStockPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columns = { "ID", "Name", "Category", "Price", "Stock", "Action" };
        DefaultTableModel stockModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable stockTable = new JTable(stockModel);
        updateStockTable(stockModel);

        JScrollPane scrollPane = new JScrollPane(stockTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton restockButton = new JButton("Restock Selected Item");
        restockButton.addActionListener(e -> {
            int row = stockTable.getSelectedRow();
            if (row >= 0) {
                String input = JOptionPane.showInputDialog("Enter quantity to add:");
                if (input != null) {
                    try {
                        int qty = Integer.parseInt(input);
                        int id = (int) stockModel.getValueAt(row, 0);
                        MenuItem item = menu.stream().filter(m -> m.getId() == id).findFirst().orElse(null);
                        if (item != null) {
                            item.addStock(qty);
                            dbManager.updateStock(item.getId(), item.getStock()); // Save to database
                            updateStockTable(stockModel);
                            updateMenuList();
                            JOptionPane.showMessageDialog(this, "Stock updated successfully!");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid quantity!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        buttonPanel.add(restockButton);

        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> updateStockTable(stockModel));
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSalesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Statistics
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 10, 10));

        JPanel ordersPanel = createStatPanel("Total Orders", String.valueOf(completedOrders.size()), Color.BLUE);
        JPanel salesPanel = createStatPanel("Total Sales", "Rp " + String.format("%,.0f", getTotalSales()),
                Color.GREEN);
        JPanel avgPanel = createStatPanel("Avg Order", "Rp " + String.format("%,.0f", getAverageOrder()), Color.ORANGE);

        statsPanel.add(ordersPanel);
        statsPanel.add(salesPanel);
        statsPanel.add(avgPanel);

        panel.add(statsPanel, BorderLayout.NORTH);

        // Best Sellers
        JPanel bestSellersPanel = new JPanel(new BorderLayout());
        bestSellersPanel.setBorder(BorderFactory.createTitledBorder("Best Sellers"));

        DefaultListModel<String> bestSellersModel = new DefaultListModel<>();
        updateBestSellers(bestSellersModel);

        JList<String> bestSellersList = new JList<>(bestSellersModel);
        bestSellersPanel.add(new JScrollPane(bestSellersList));

        panel.add(bestSellersPanel, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("🔄 Refresh Report");
        refreshButton.addActionListener(e -> {
            ordersPanel.removeAll();
            ordersPanel.add(createStatPanel("Total Orders", String.valueOf(completedOrders.size()), Color.BLUE));
            salesPanel.removeAll();
            salesPanel
                    .add(createStatPanel("Total Sales", "Rp " + String.format("%,.0f", getTotalSales()), Color.GREEN));
            avgPanel.removeAll();
            avgPanel.add(createStatPanel("Avg Order", "Rp " + String.format("%,.0f", getAverageOrder()), Color.ORANGE));
            updateBestSellers(bestSellersModel);
            panel.revalidate();
            panel.repaint();
        });
        panel.add(refreshButton, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatPanel(String title, String value, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(color, 2));
        panel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 20));
        valueLabel.setForeground(color);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private void addToCart(MenuItem item) {
        try {
            if (item.getStock() <= 0) {
                throw new OutOfStockException(item.getName() + " is out of stock!");
            }

            currentOrder.addItem(item, 1);
            updateCartDisplay();
            JOptionPane.showMessageDialog(this, item.getName() + " added to cart!");
        } catch (OutOfStockException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCartDisplay() {
        cartTableModel.setRowCount(0);

        for (Order.OrderItem item : currentOrder.getItems()) {
            Object[] row = {
                    item.getMenuItem().getName(),
                    String.format("Rp %,.0f", item.getMenuItem().getPrice()),
                    item.getQuantity(),
                    String.format("Rp %,.0f", item.getSubtotal())
            };
            cartTableModel.addRow(row);
        }

        totalLabel.setText(String.format("Rp %,.0f", currentOrder.getTotal()));
        taxLabel.setText(String.format("Rp %,.0f", currentOrder.getTax()));
        grandTotalLabel.setText(String.format("Rp %,.0f", currentOrder.getGrandTotal()));
    }

    private void updateCartQuantity(int row) {
        try {
            int newQty = Integer.parseInt(cartTableModel.getValueAt(row, 2).toString());
            Order.OrderItem item = currentOrder.getItems().get(row);

            if (newQty > item.getMenuItem().getStock()) {
                throw new OutOfStockException("Only " + item.getMenuItem().getStock() + " available!");
            }

            currentOrder.updateQuantity(item.getMenuItem().getId(), newQty);
            updateCartDisplay();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            updateCartDisplay(); // Reset display
        }
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row >= 0) {
            Order.OrderItem item = currentOrder.getItems().get(row);
            currentOrder.removeItem(item.getMenuItem().getId());
            updateCartDisplay();
        }
    }

    private void clearCart() {
        currentOrder = new Order();
        updateCartDisplay();
    }

    private void processPayment() {
        try {
            if (currentOrder.getItems().isEmpty()) {
                throw new InvalidPaymentException("Cart is empty!");
            }

            double amount = Double.parseDouble(paymentField.getText());
            String method = (String) paymentMethodCombo.getSelectedItem();

            double change = currentOrder.processPayment(amount, method);

            // Show receipt
            JTextArea receiptArea = new JTextArea(currentOrder.getReceipt());
            receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            receiptArea.setEditable(false);

            JOptionPane.showMessageDialog(this, new JScrollPane(receiptArea),
                    "Receipt", JOptionPane.INFORMATION_MESSAGE);

            JOptionPane.showMessageDialog(this,
                    String.format("Payment successful!\nChange: Rp %,.0f", change),
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            // Save order to database
            int savedOrderId = dbManager.saveOrder(currentOrder);
            if (savedOrderId > 0) {
                currentOrder.setOrderId(savedOrderId);
            }
            completedOrders.add(currentOrder);

            // Update stock in database for all items
            for (Order.OrderItem orderItem : currentOrder.getItems()) {
                dbManager.updateStock(orderItem.getMenuItem().getId(), orderItem.getMenuItem().getStock());
            }

            // Reset
            currentOrder = new Order();
            updateCartDisplay();
            updateMenuList();
            paymentField.setText("");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid payment amount!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMenuList() {
        menuListModel.clear();
        for (MenuItem item : menu) {
            menuListModel.addElement(item);
        }
    }

    private void updateStockTable(DefaultTableModel model) {
        model.setRowCount(0);
        for (MenuItem item : menu) {
            Object[] row = {
                    item.getId(),
                    item.getName(),
                    item.getCategory(),
                    String.format("Rp %,.0f", item.getPrice()),
                    item.getStock() + (item.getStock() < 10 ? " ⚠️" : ""),
                    "Restock"
            };
            model.addRow(row);
        }
    }
    private double getTotalSales() {
        double total = 0;
        for (Order order : completedOrders) {
            total += order.getGrandTotal();
        }
        return total;
    }
    private double getAverageOrder() {
        if (completedOrders.isEmpty())
            return 0;
        return getTotalSales() / completedOrders.size();
    }
    private void updateBestSellers(DefaultListModel<String> model) {
        model.clear();

        HashMap<String, Integer> sales = new HashMap<>();
        for (Order order : completedOrders) {
            for (Order.OrderItem item : order.getItems()) {
                String name = item.getMenuItem().getName();
                sales.put(name, sales.getOrDefault(name, 0) + item.getQuantity());
            }
        }
        sales.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .forEach(entry -> model.addElement(entry.getKey() + " - " + entry.getValue() + " sold"));

        if (model.isEmpty()) {
            model.addElement("No sales data yet");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new POSRestaurant());
    }
}

