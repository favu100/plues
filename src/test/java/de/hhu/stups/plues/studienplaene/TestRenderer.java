package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;


public class TestRenderer extends TestBase {

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {
    super.setUp();
  }

  @Test
  public void testItWorksForColor() throws RenderingException, IOException {
    final Renderer renderer = new Renderer(store, result, course, ColorChoice.COLOR);
    final ByteArrayOutputStream result = renderer.getResult();

    File pdf = File.createTempFile("color", ".pdf");
    try (FileOutputStream outputStream = new FileOutputStream(pdf.getAbsoluteFile())) {
      result.writeTo(outputStream);
    }
    assertNotNull(result);
  }

  @Test
  public void testItWorksForGrayscale() throws RenderingException, IOException {
    final Renderer renderer = new Renderer(store, result, course, ColorChoice.GRAYSCALE);
    final ByteArrayOutputStream result = renderer.getResult();

    File pdf = File.createTempFile("gray", ".pdf");
    try (FileOutputStream stream = new FileOutputStream(pdf.getAbsoluteFile())) {
      result.writeTo(stream);
    }
    assertNotNull(result);
  }
}
