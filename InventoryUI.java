import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.Vector;
import javax.swing.*;

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

    public InventoryUI() {
        try {
         
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3340/invent", "root", "root");
            status = true;

           
            setTitle("QuickStock");
            setSize(700, 500);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            
            initUI();

       
            fetchData();

     
            setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to connect database: " + e.getMessage());
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
        JButton exportButton = new JButton("Export to CSV");
        JButton nextPageButton = new JButton("Next");
        JButton prevPageButton = new JButton("Previous");




        insertButton.addActionListener(e -> insertData());
        updateButton.addActionListener(e -> updateData());
        deleteButton.addActionListener(e -> deleteData());
        searchButton.addActionListener(e -> searchData());
        viewAllButton.addActionListener(e -> fetchData()); 
        exportButton.addActionListener(e -> exportToCSV());
        
        

    
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Quantity:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(quantityField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Price:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(priceField, gbc);

      
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(insertButton, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(updateButton, gbc);

        gbc.gridx = 2; gbc.gridy = 3;
        panel.add(deleteButton, gbc);

       
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        searchPanel.add(new JLabel("Search by Name:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(viewAllButton); 

        searchPanel.add(exportButton);
        
        JButton sortByPrice = new JButton("Sort by price");
        JButton sortByQuantity = new JButton("Sort by quantity");
        sortByPrice.addActionListener(e -> {
            orderby="ORDER BY price ASC";
            fetchData();
        });        
        sortByQuantity.addActionListener(e->{
            orderby="ORDER BY quantity ASC";
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

    private void fetchData() {
        try {
            int offset = (currentPage - 1) * rowsPerPage;
            String query = "SELECT * FROM inventory "+orderby+" LIMIT ? OFFSET ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
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
    
            if(!rs.next()){
                JOptionPane.showMessageDialog(this, "End of results!");
                currentPage--;
                return;
            }

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
            JOptionPane.showMessageDialog(this, "Error fetching data: " + e.getMessage());
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

   
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryUI());
    }
}
