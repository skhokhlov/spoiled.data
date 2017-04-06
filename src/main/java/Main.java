import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

import com.google.gson.Gson;

import static spark.Spark.*;

public class Main {

    private static Connection Connection = null;

    private static void initialiseDB() {
        Statement stmt;
        try {
            Class.forName("org.postgresql.Driver");
            Connection = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/docker", "docker", "docker");
            stmt = Connection.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS LIST " +
                    "(id SERIAL PRIMARY KEY NOT NULL," +
                    "name VARCHAR(200)," +
                    "phone INT NOT NULL," +
                    "expired TIME NOT NULL);";
            stmt.executeUpdate(sql);
            stmt.close();

            Connection.setAutoCommit(false);

        } catch (Exception e) {
            throw new Error(e.getClass().getName() + ": " + e.getMessage());

        }
    }

    private static int insertItem(ListItem data) {
        Statement stmt;
        try {
            stmt = Connection.createStatement();
            String sql = "INSERT INTO LIST (NAME,PHONE,EXPIRED) "
                    + "VALUES ('" + data.Name + "', " + data.Phone + ", " + data.Expired + " );";

            stmt.executeUpdate(sql);

            stmt.close();
            Connection.commit();

            return 0;
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());

            return 1;
        }
    }

    private static int updateItem(ListItem data) {
        Statement stmt;
        try {
            stmt = Connection.createStatement();
            String sql = "UPDATE LIST (ID,NAME,PHONE) "
                    + "VALUES (" + data.Id + ", '" + data.Name + "', " + data.Phone + " );";
            stmt.executeUpdate(sql);

            stmt.close();
            Connection.commit();

            return 0;
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());

            return 1;
        }
    }

    private static ArrayList<ListItem> getItems() {
        Statement stmt;
        ArrayList<ListItem> result = new ArrayList<>();

        try {
            stmt = Connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM LIST;");

            while (rs.next()) {
                result.add(new ListItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("phone"),
                        rs.getDate("expired")
                ));
            }
            rs.close();
            stmt.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());

        }

        return result;
    }

    private static ListItem getItem(int id) {
        Statement stmt;
        ListItem result = null;

        try {
            stmt = Connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM LIST WHERE ID = " + id + ";");


            result = new ListItem(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("phone"),
                    rs.getDate("expired")
            );

            rs.close();
            stmt.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());

        }

        if (result == null) {
            result = new ListItem(-1);
        }

        return result;
    }

    private static class ListItem {
        int Id = -1;
        String Name = null;
        int Phone = 0;
        Date Expired = null;


        ListItem(int inputId) {
            Id = inputId;
        }

        ListItem(int inputId, String inputName, int inputPhone, Date inputExpired) {
            Id = inputId;
            Name = inputName;
            Phone = inputPhone;
            Expired = inputExpired;
        }

        ListItem(String inputName, int inputPhone) {
            Name = inputName;
            Phone = inputPhone;
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

        get("/items/:id", (req, res) -> {
            res.type("application/json");
            Gson gson = new Gson();
            ListItem response = getItem(Integer.parseInt(req.params(":id")));

            if (response.Id == -1) {
                HttpStatus responseError = new HttpStatus(404);
                return gson.toJson(responseError);
            } else {
                return gson.toJson(response);
            }

        });

        post("/items/:id", (req, res) -> {
            res.type("application/json");
            Gson gson = new Gson();
            ListItem item = gson.fromJson(req.body(), ListItem.class);
            item.Id = Integer.parseInt(req.params(":id"));
            HttpStatus response;

            if (updateItem(item) == 0) {
                response = new HttpStatus(200);
            } else {
                response = new HttpStatus(400);
                res.status(400);
            }

            return gson.toJson(response);
        });

        post("/items/add", (req, res) -> {
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

