import net.sf.json.JSONObject;
import tech.kuaida.sqlbuilder.SelectBuilder;

public class Main {
    public static void main(String[] args) {
        String json = "{salesperson: {name:null, phone:null}}";

        JSONObject jsonObject = JSONObject.fromObject(json);

        SelectBuilder selectBuilder = new SelectBuilder("modules", jsonObject);
        System.out.println(selectBuilder.toString());
    }
}
