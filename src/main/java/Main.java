import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

import com.google.gson.Gson;

import static spark.Spark.*;

public class Main {

    private static Connection db() {
        Connection c = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/docker", "docker", "docker");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }

        return c;
    }

    private static int initialiseDB() {
        Connection c = db();
        Statement stmt;
        try {
            stmt = c.createStatement();
            String sql = "CREATE TABLE LIST " +
                    "(ID SERIAL PRIMARY KEY     NOT NULL," +
                    " NAME           TEXT    NOT NULL, " +
                    " NUMBER            INT     NOT NULL )";
            stmt.executeUpdate(sql);
            stmt.close();

            c.close();

            return 0;
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());

            return 1;
        }
    }

    private static int insertItem(ListItem data) {
        Connection c = db();
        Statement stmt;
        try {
            c.setAutoCommit(false);

            stmt = c.createStatement();
            String sql = "INSERT INTO LIST (NAME,NUMBER) "
                    + "VALUES ('" + data.Name + "', " + data.Number + " );";
            stmt.executeUpdate(sql);

            stmt.close();
            c.commit();
            c.close();

            return 0;
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());

            return 1;
        }
    }

    private static ArrayList<ListItem> getItems() {
        Connection c = db();
        Statement stmt;
        ArrayList<ListItem> result = new ArrayList<>();

        try {
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

        }

        return result;
    }

    private static class ListItem {
        int Id;
        String Name;
        int Number;

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

    private static class HttpStatus {
        int Status;
        String Message;

        HttpStatus(int inputStatus, String inputMessage) {
            Status = inputStatus;
            Message = inputMessage;
        }

        HttpStatus(int inputStatus) {
            Status = inputStatus;
        }
    }

    // docker run --rm -p 5432:5432 --name pg_test eg_postgresql
    public static void main(String[] args) {
        initialiseDB();

        after((request, response) -> response.header("Content-Encoding", "gzip"));

        notFound((req, res) -> {
            res.type("application/json");
            Gson gson = new Gson();
            HttpStatus response = new HttpStatus(404, "Not Found");
            return gson.toJson(response);
        });

        internalServerError((req, res) -> {
            res.type("application/json");
            Gson gson = new Gson();
            HttpStatus response = new HttpStatus(500, "Internal server error");
            return gson.toJson(response);
        });

        get("/status", (req, res) -> "Works");

        get("/items/everything", (req, res) -> {
            res.type("application/json");
            Gson gson = new Gson();
            ArrayList<ListItem> b = getItems();
            return gson.toJson(b);
        });

        post("/items/:item", (req, res) -> {
            res.type("application/json");
            Gson gson = new Gson();
            ListItem item = gson.fromJson(req.body(), ListItem.class);
            HttpStatus response;

            if (insertItem(item) == 0) {
                response = new HttpStatus(200);
            } else {
                response = new HttpStatus(400);
                res.status(400);
            }

            return gson.toJson(response);
        });


    }


}

