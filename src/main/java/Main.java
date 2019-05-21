import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import lombok.Data;
import lombok.Value;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.jeasy.random.api.Randomizer;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Value
class Person {
    private String fName;
    private String lName;
    private UUID uuid;

    @JsonCreator
    Person(@JsonProperty("fName") String fName,
           @JsonProperty("lName") String lName,
           @JsonProperty("uuid") UUID uuid) {
        this.fName = fName;
        this.lName = lName;
        this.uuid = uuid;
    }
}

@Data
class PersonDto {
    private String firstName;
    private String lastName;
}

@Mapper
interface PersonMapper {

    PersonMapper INSTANCE = Mappers.getMapper(PersonMapper.class);

    @Mapping(source = "FName", target = "firstName")
    @Mapping(source = "LName", target = "lastName")
    PersonDto toDto(Person p);
}

class Util {

    // @formatter:off
    private static final EasyRandom ER;
    static {
        EasyRandomParameters erp = new EasyRandomParameters()
                .randomize(
                        FieldPredicates.named("fName").and(FieldPredicates.ofType(String.class)),
                        stringRandomizer("frank", "jenny",0.7d))
                .randomize(
                        FieldPredicates.named("lName"), stringRandomizer("blank", "page",0.7d))
                .stringLengthRange(5, 10);
        ER = new EasyRandom(erp);
    }
    // @formatter:on

    private static Randomizer<String> stringRandomizer(String left, String right, double threshold) {
        return () -> {
            double random = Math.random();
            return random >= threshold ? left : right;
        };
    }

    //@formatter:off
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    //@formatter:on

    static <T> String serialize(@NotNull Collection<T> args) throws JsonProcessingException {
        return objectMapper.writeValueAsString(args);
    }

    static void configureEndpoints(Javalin server, List<Person> persons) {
        server.get("/", ctx -> ctx.result(serialize(persons)));
        server.get("/:id", ctx ->
                ctx.json(PersonMapper.INSTANCE.toDto(persons.get(Integer.parseInt(ctx.pathParam("id"))))));

        server.post("/", ctx ->
                persons.add(ctx.bodyAsClass(Person.class)));
        server.delete("/:id", ctx ->
                persons.remove(Integer.parseInt(ctx.pathParam("id"))));
    }

    static List<Person> getPersons(int count) {
        return ER.objects(Person.class, count).collect(Collectors.toList());
    }
}

public class Main {

    public static void main(String[] args) {

        List<Person> persons = Util.getPersons(10);
        Javalin server = Javalin.create().start(7000);
        Util.configureEndpoints(server, persons);
    }

}
