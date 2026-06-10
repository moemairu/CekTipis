class Beverage extends MenuItem {
    private boolean isHot;

    public Beverage(String name, double price, int stock, boolean isHot) {
        super(name, price, stock, "Beverage");
        this.isHot = isHot;
    }

    @Override
    public String getDescription() {
        String temp = isHot ? "☕ Hot" : "🧊 Cold";
        return temp + " " + getName() + " - Rp " + String.format("%,.0f", getPrice());
    }
}