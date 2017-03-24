import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

import static spark.Spark.*;

public class Main {

    private static void initialiseDB() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/docker",
                            "docker", "docker");

            stmt = c.createStatement();
            String sql = "CREATE TABLE LIST " +
                    "(ID INT PRIMARY KEY     NOT NULL," +
                    " NAME           TEXT    NOT NULL, " +
                    " NUMBER            INT     NOT NULL )";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
//            System.exit(0);
        }
    }

    private static void insertItem(ListItem data) {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/docker",
                            "docker", "docker");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String sql = "INSERT INTO LIST (ID,NAME,NUMBER) "
                    + "VALUES (" + data.Id + ", '" + data.Name + "', " + data.Number + " );";
            stmt.executeUpdate(sql);

            stmt.close();
            c.commit();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    private static ArrayList<ListItem> getItems() {
        Connection c = null;
        Statement stmt = null;
        ArrayList<ListItem> result = new ArrayList<>();

        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/docker",
                            "docker", "docker");
            c.setAutoCommit(false);

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM LIST;");

            while (rs.next()) {
                result.add(new ListItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("number")
                ));
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }

        return result;
    }

    private static class ListItem {
        public int Id;
        public String Name;
        public int Number;

        ListItem(int inputId, String inputName, int inputNumber) {
            Id = inputId;
            Name = inputName;
            Number = inputNumber;
        }

        ListItem(String inputName, int inputNumber) {
            Name = inputName;
            Number = inputNumber;
        }
    }

    // docker run --rm -p 5432:5432 --name pg_test eg_postgresql
    public static void main(String[] args) {
        initialiseDB();
        ListItem a = new ListItem("ASD", 123);
        insertItem(a);
        ArrayList<ListItem> b = getItems();
        System.out.println(b.get(0).Name);

        get("/status", (req, res) -> "Works");

    }


}

