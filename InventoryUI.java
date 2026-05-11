
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.Vector;
import javax.swing.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class InventoryUI extends JFrame {

    private Connection conn;
    private boolean status;
    private int currentPage = 1;
    private int rowsPerPage = 10;
    private JLabel totalPagesLabel;
    private String orderby = "ORDER BY id";
    private JTable inventoryTable;
    private JScrollPane scrollPane;
    private JTextField nameField, quantityField, priceField, searchField;
    private JButton insertButton, updateButton, deleteButton, searchButton, viewAllButton;

    private boolean showLoginDialog() {

        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        Object[] message = {
            "Username:", usernameField,
            "Password:", passwordField
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                message,
                "Login",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option == JOptionPane.OK_OPTION) {

            String username = usernameField.getText();
            String password = String.valueOf(passwordField.getPassword());

            if (username.equals("admin")
                    && password.equals("admin")) {

                return true;

            } else {

                JOptionPane.showMessageDialog(
                        this,
                        "Invalid Username or Password"
                );

                return false;
            }
        }

        return false;
    }

    public InventoryUI() {

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            // Connect first
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/",
                    "root",
                    "root"
            );

            // Then setup database/table
            setupDatabase();

            // Reconnect with database selected
            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/invent",
                    "root",
                    "root"
            );

            status = true;

            setTitle("QuickStock");
            setSize(1000, 700);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            initUI();

            fetchData();

            if (showLoginDialog()) {

                setVisible(true);

            } else {

                dispose();
            }
        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Failed:\n" + e.getMessage()
            );
        }
    }

    private void initUI() {

        setLayout(new BorderLayout());
        ImageIcon icon = new ImageIcon("logo.png");
        setIconImage(icon.getImage());

        JPanel headingPanel = new JPanel();
        headingPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JLabel headingLabel = new JLabel("QuickStock Inventory");
        headingLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headingPanel.add(headingLabel);

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        nameField = new JTextField(10);
        quantityField = new JTextField(10);
        priceField = new JTextField(10);
        searchField = new JTextField(15);

        insertButton = new JButton("Insert");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        searchButton = new JButton("Search");
        viewAllButton = new JButton("View All");
        JButton pieChartButton = new JButton("Pie Chart");
        JButton barChartButton = new JButton("Bar Chart");
        JButton exportButton = new JButton("Export to CSV");
        JButton nextPageButton = new JButton("Next");
        JButton prevPageButton = new JButton("Previous");

        insertButton.addActionListener(e -> insertData());
        updateButton.addActionListener(e -> updateData());
        deleteButton.addActionListener(e -> deleteData());
        searchButton.addActionListener(e -> searchData());
        viewAllButton.addActionListener(e -> fetchData());
        exportButton.addActionListener(e -> exportToCSV());
        pieChartButton.addActionListener(e -> showPieChart());
        barChartButton.addActionListener(e -> showBarChart());
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Quantity:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(quantityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Price:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(priceField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(insertButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(updateButton, gbc);

        gbc.gridx = 2;
        gbc.gridy = 3;
        panel.add(deleteButton, gbc);

        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        searchPanel.add(new JLabel("Search by Name:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(viewAllButton);

        searchPanel.add(exportButton);
        searchPanel.add(pieChartButton);
        searchPanel.add(barChartButton);
        JButton sortByPrice = new JButton("Sort by price");
        JButton sortByQuantity = new JButton("Sort by quantity");
        sortByPrice.addActionListener(e -> {
            orderby = "ORDER BY price ASC";
            fetchData();
        });
        sortByQuantity.addActionListener(e -> {
            orderby = "ORDER BY quantity ASC";
            fetchData();
        });

        JPanel paginationPanel = new JPanel();
        JButton prevButton = new JButton("Prev");
        JButton nextButton = new JButton("Next");
        paginationPanel.add(prevButton);
        paginationPanel.add(nextButton);
        totalPagesLabel = new JLabel("Page " + currentPage + " of ");
        paginationPanel.add(sortByPrice);
        paginationPanel.add(sortByQuantity);
        paginationPanel.add(totalPagesLabel);

        updateTotalPages();

        nextButton.addActionListener(e -> {
            currentPage++;
            fetchData();
            updateTotalPages();
        });

        prevButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                fetchData();
                updateTotalPages();
            }
        });

        inventoryTable = new JTable();
        scrollPane = new JScrollPane(inventoryTable);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(headingPanel, BorderLayout.NORTH);
        mainPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(searchPanel, BorderLayout.SOUTH);
        add(mainPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(paginationPanel, BorderLayout.SOUTH);
    }

    private void updateTotalPages() {
        try {
            String query = "SELECT COUNT(*) FROM inventory";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                int totalRecords = rs.getInt(1);
                int totalPages = (int) Math.ceil((double) totalRecords / rowsPerPage);
                totalPagesLabel.setText("Page " + currentPage + " of " + totalPages);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error calculating total pages: " + e.getMessage());
        }
    }

    private void setupDatabase() {

        try {

            Statement stmt = conn.createStatement();

            // Create Database
            stmt.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS invent"
            );

            // Select Database
            stmt.execute("USE invent");

            // Create Table
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS inventory ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "quantity INT NOT NULL,"
                    + "price DOUBLE NOT NULL"
                    + ")"
            );

            System.out.println(
                    "Database and table checked/created successfully!"
            );

        } catch (SQLException e) {

            System.out.println(
                    "DATABASE SETUP ERROR"
            );

            e.printStackTrace();
        }
    }

    private void fetchData() {

        try {

            if (currentPage < 1) {
                currentPage = 1;
            }

            int offset = (currentPage - 1) * rowsPerPage;

            String query
                    = "SELECT * FROM inventory "
                    + orderby
                    + " LIMIT ? OFFSET ?";

            PreparedStatement pstmt
                    = conn.prepareStatement(query);

            pstmt.setInt(1, rowsPerPage);
            pstmt.setInt(2, offset);

            ResultSet rs = pstmt.executeQuery();

            Vector<Vector<Object>> data = new Vector<>();
            Vector<String> columnNames = new Vector<>();

            columnNames.add("ID");
            columnNames.add("Name");
            columnNames.add("Quantity");
            columnNames.add("Price");
            columnNames.add("Total");

            while (rs.next()) {

                Vector<Object> row = new Vector<>();

                row.add(rs.getInt("id"));
                row.add(rs.getString("name"));
                row.add(rs.getInt("quantity"));
                row.add(rs.getDouble("price"));
                row.add(
                        rs.getInt("quantity")
                        * rs.getDouble("price")
                );

                data.add(row);
            }

            if (data.isEmpty()) {

                JOptionPane.showMessageDialog(
                        this,
                        "No more records!"
                );

                if (currentPage > 1) {
                    currentPage--;
                }

                return;
            }

            inventoryTable.setModel(
                    new javax.swing.table.DefaultTableModel(
                            data,
                            columnNames
                    )
            );

        } catch (SQLException e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Error fetching data:\n" + e.getMessage()
            );
        }
    }

    private void insertData() {
        try {
            String name = nameField.getText();
            int quantity = Integer.parseInt(quantityField.getText());
            double price = Double.parseDouble(priceField.getText());

            String query = "INSERT INTO inventory (name, quantity, price) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, name);
            pstmt.setInt(2, quantity);
            pstmt.setDouble(3, price);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Data inserted successfully!");
            fetchData();
            quantityField.setText("");
            priceField.setText("");
            nameField.setText("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error inserting data: " + e.getMessage());
        }
    }

    private void exportToCSV() {
        try {
            String query = "SELECT * FROM inventory";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            FileWriter csvWriter = new FileWriter("InventoryData.csv");

            // Write header
            csvWriter.append("ID,Name,Quantity,Price,Total\n");

            // Write data rows
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int quantity = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double total = quantity * price;

                csvWriter.append(id + "," + name + "," + quantity + "," + price + "," + total + "\n");
            }

            csvWriter.flush();
            csvWriter.close();

            JOptionPane.showMessageDialog(this, "Data exported to InventoryData.csv successfully!");

        } catch (SQLException | IOException e) {
            JOptionPane.showMessageDialog(this, "Error exporting to CSV: " + e.getMessage());
        }
    }

    private void updateData() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog("Enter ID to update:"));
            String name = nameField.getText();
            int quantity = Integer.parseInt(quantityField.getText());
            double price = Double.parseDouble(priceField.getText());

            String query = "UPDATE inventory SET name = ?, quantity = ?, price = ? WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, name);
            pstmt.setInt(2, quantity);
            pstmt.setDouble(3, price);
            pstmt.setInt(4, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Data updated successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "No record found with ID " + id);
            }
            fetchData();
            quantityField.setText("");
            priceField.setText("");
            nameField.setText("");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating data: " + e.getMessage());
        }
    }

    private void deleteData() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog("Enter ID to delete:"));
            String query = "DELETE FROM inventory WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Data deleted successfully!");
            } else {
                JOptionPane.showMessageDialog(this, "No record found with ID " + id);
            }
            fetchData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting data: " + e.getMessage());
        }
    }

    private void searchData() {
        try {
            String name = searchField.getText();
            String query = "SELECT * FROM inventory WHERE name LIKE ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();

            Vector<Vector<Object>> data = new Vector<>();
            Vector<String> columnNames = new Vector<>();
            columnNames.add("ID");
            columnNames.add("Name");
            columnNames.add("Quantity");
            columnNames.add("Price");
            columnNames.add("Total");

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("id"));
                row.add(rs.getString("name"));
                row.add(rs.getInt("quantity"));
                row.add(rs.getDouble("price"));
                row.add(rs.getInt("quantity") * rs.getDouble("price"));
                data.add(row);
            }

            inventoryTable.setModel(new javax.swing.table.DefaultTableModel(data, columnNames));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error searching data: " + e.getMessage());
        }
    }

    private void showPieChart() {

        try {

            DefaultPieDataset dataset
                    = new DefaultPieDataset();

            String query
                    = "SELECT name, quantity FROM inventory";

            Statement stmt
                    = conn.createStatement();

            ResultSet rs
                    = stmt.executeQuery(query);

            while (rs.next()) {

                dataset.setValue(
                        rs.getString("name"),
                        rs.getInt("quantity")
                );
            }

            JFreeChart chart
                    = ChartFactory.createPieChart(
                            "Inventory Quantity",
                            dataset,
                            true,
                            true,
                            false
                    );

            ChartFrame frame
                    = new ChartFrame(
                            "Pie Chart",
                            chart
                    );

            frame.setSize(600, 600);
            frame.setVisible(true);

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage()
            );
        }
    }

    private void showBarChart() {

        try {

            DefaultCategoryDataset dataset
                    = new DefaultCategoryDataset();

            String query
                    = "SELECT name, quantity FROM inventory";

            Statement stmt
                    = conn.createStatement();

            ResultSet rs
                    = stmt.executeQuery(query);

            while (rs.next()) {

                dataset.addValue(
                        rs.getInt("quantity"),
                        "Stock",
                        rs.getString("name")
                );
            }

            JFreeChart chart
                    = ChartFactory.createBarChart(
                            "Inventory Stock",
                            "Product",
                            "Quantity",
                            dataset
                    );

            ChartFrame frame
                    = new ChartFrame(
                            "Bar Chart",
                            chart
                    );

            frame.setSize(800, 600);
            frame.setVisible(true);

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage()
            );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryUI());
    }
}
