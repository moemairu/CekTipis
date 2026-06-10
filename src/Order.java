import java.util.ArrayList;
import java.util.Date;

class Order {
    private int orderId;
    private ArrayList<OrderItem> items;
    private Date timestamp;
    private String status;
    private String paymentMethod;
    private double paymentAmount;

    // Inner Class
    class OrderItem {
        private MenuItem menuItem;
        private int quantity;

        public OrderItem(MenuItem menuItem, int quantity) {
            this.menuItem = menuItem;
            this.quantity = quantity;
        }

        public MenuItem getMenuItem() { return menuItem; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int qty) { this.quantity = qty; }

        public double getSubtotal() {
            return menuItem.getPrice() * quantity;
        }
    }

    public Order() {
        this.items = new ArrayList<>();
        this.timestamp = new Date();
        this.status = "Pending";
    }

    public void addItem(MenuItem menuItem, int quantity) {
        // Check if item already exists
        for (OrderItem item : items) {
            if (item.getMenuItem().getId() == menuItem.getId()) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        items.add(new OrderItem(menuItem, quantity));
    }

    public void removeItem(int menuItemId) {
        items.removeIf(item -> item.getMenuItem().getId() == menuItemId);
    }
    public void updateQuantity(int menuItemId, int newQuantity) throws InvalidQuantityException {
        if (newQuantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than 0");
        }

        for (OrderItem item : items) {
            if (item.getMenuItem().getId() == menuItemId) {
                item.setQuantity(newQuantity);
                return;
            }
        }
    }

    public double getTotal() {
        double total = 0;
        for (OrderItem item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    public double getTax() {
        return getTotal() * 0.10; // 10% tax
    }
    public double getGrandTotal() {
        return getTotal() + getTax();
    }
    public double processPayment(double amount, String method) throws InvalidPaymentException, OutOfStockException {
        double total = getGrandTotal();

        if (amount < total) {
            throw new InvalidPaymentException("Insufficient payment! Required: Rp " + String.format("%,.0f", total));
        }

        // Reduce stock for all items
        for (OrderItem item : items) {
            item.getMenuItem().reduceStock(item.getQuantity());
        }

        this.status = "Completed";
        this.paymentMethod = method;
        this.paymentAmount = amount;
        return amount - total; // Change
    }

    public String getReceipt() {
        StringBuilder receipt = new StringBuilder();
        receipt.append("\n╔════════════════════════════════════╗\n");
        receipt.append("║     WARUNG PADANG SEDERHANA       ║\n");
        receipt.append("║      Jl. Merdeka No. 123          ║\n");
        receipt.append("╠════════════════════════════════════╣\n");
        receipt.append(String.format("  Order #%04d\n", orderId));
        receipt.append("  " + timestamp.toString() + "\n");
        receipt.append("────────────────────────────────────\n");

        for (OrderItem item : items) {
            String name = String.format("%-20s", item.getMenuItem().getName());
            String qty = String.format("%4dx", item.getQuantity());
            String price = String.format("%12s", "Rp " + String.format("%,.0f", item.getSubtotal()));
            receipt.append(name + " " + qty + " " + price + "\n");
        }

        receipt.append("────────────────────────────────────\n");
        receipt.append(String.format("%-30s %12s\n", "Subtotal:", "Rp " + String.format("%,.0f", getTotal())));
        receipt.append(String.format("%-30s %12s\n", "Tax (10%):", "Rp " + String.format("%,.0f", getTax())));
        receipt.append("────────────────────────────────────\n");
        receipt.append(String.format("%-30s %12s\n", "TOTAL:", "Rp " + String.format("%,.0f", getGrandTotal())));
        receipt.append(String.format("%-30s %12s\n", "Payment (" + paymentMethod + "):", "Rp " + String.format("%,.0f", paymentAmount)));
        receipt.append(String.format("%-30s %12s\n", "Change:", "Rp " + String.format("%,.0f", paymentAmount - getGrandTotal())));
        receipt.append("────────────────────────────────────\n");
        receipt.append("    Terima Kasih! 🙏\n");
        receipt.append("    Selamat Menikmati! 😋\n");
        receipt.append("╚════════════════════════════════════╝\n");
        return receipt.toString();
    }

    public ArrayList<OrderItem> getItems() { return items; }
    public int getOrderId() { return orderId; }
    public Date getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public String getPaymentMethod() { return paymentMethod; }
    public double getPaymentAmount() { return paymentAmount; }

    // Setters for loading from database
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setStatus(String status) { this.status = status; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public void setPaymentAmount(double paymentAmount) { this.paymentAmount = paymentAmount; }

    // Add item without checking for duplicates (for loading from database)
    public void addItemForLoading(MenuItem menuItem, int quantity) {
        items.add(new OrderItem(menuItem, quantity));
    }
}
