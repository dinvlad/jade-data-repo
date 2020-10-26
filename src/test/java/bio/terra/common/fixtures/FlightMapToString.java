package bio.terra.common.fixtures;

import bio.terra.common.category.Unit;
import bio.terra.stairway.FlightMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.validation.constraints.*;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@Category(Unit.class)
public class FlightMapToString {

    @Test
    public void hasValue() {
        FlightMap mp = new FlightMap();
        mp.put("key", "value");
        mp.put("adslfjasdlfjadsf", "lja;dslfkjas;dlkfjasdf");
        String expectedOutput = "{adslfjasdlfjadsf=lja;dslfkjas;dlkfjasdf, key=value}";
        String flightMapString = mp.toString();
        System.out.println(flightMapString);
        assertThat("Correctly formatted flight map string", flightMapString, equalTo(expectedOutput));

    @Test
    public void handlesNull() {
        FlightMap mp = new FlightMap();
        mp.put("key", null);
        mp.put("adslfjasdlfjadsf", "lja;dslfkjas;dlkfjasdf");
        String expectedOutput = "{adslfjasdlfjadsf=lja;dslfkjas;dlkfjasdf, key=null}";
        String flightMapString = mp.toString();
        System.out.println(flightMapString);
        assertThat("Correctly formatted flight map string", flightMapString, equalTo(expectedOutput));
    }

}
