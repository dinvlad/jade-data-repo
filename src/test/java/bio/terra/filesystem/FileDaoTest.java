package bio.terra.filesystem;

import bio.terra.category.Connected;
import bio.terra.filesystem.exception.FileSystemCorruptException;
import bio.terra.filesystem.exception.FileSystemObjectDependencyException;
import bio.terra.filesystem.exception.FileSystemObjectNotFoundException;
import bio.terra.filesystem.exception.InvalidFileSystemObjectTypeException;
import bio.terra.fixtures.ConnectedOperations;
import bio.terra.fixtures.Names;
import bio.terra.metadata.FSDir;
import bio.terra.metadata.FSFile;
import bio.terra.metadata.FSFileInfo;
import bio.terra.metadata.FSObjectBase;
import bio.terra.metadata.FSObjectType;
import bio.terra.metadata.Study;
import bio.terra.model.BillingProfileModel;
import bio.terra.model.StudySummaryModel;
import bio.terra.resourcemanagement.service.google.GoogleResourceConfiguration;
import bio.terra.service.SamClientService;
import bio.terra.service.StudyService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"google", "connectedtest"})
@Category(Connected.class)
public class FileDaoTest {
    private final Logger logger = LoggerFactory.getLogger("bio.terra.filesystem.FileDaoTest");

    private static final String mimeType = "application/octet-string";
    private static final String description = "dummy description";

    private String topDir = Names.randomizeName("top");
    private String secondDir = Names.randomizeName("second");
    private String thirdDir = Names.randomizeName("third");
    private String fileA = Names.randomizeName("fileA_") + ".ext";
    private String fileB = Names.randomizeName("fileB_") + ".ext";

    private String topPath = "/" + topDir;
    private String secondPath = topPath + "/" + secondDir;
    private String thirdPath = secondPath + "/" + thirdDir;
    private String fileAPath = thirdPath + "/" + fileA;
    private String fileBPath = thirdPath + "/" + fileB;

    private UUID studyId;
    private String flightId = UUID.randomUUID().toString();
    private String datasetId = UUID.randomUUID().toString();
    private StudySummaryModel studySummaryModel;
    private BillingProfileModel billingProfileModel;
    private Study study;

    @Autowired
    private FireStoreFileDao fileDao;

    @Autowired
    private FireStoreDependencyDao dependencyDao;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private ConnectedOperations connectedOperations;

    @Autowired
    private GoogleResourceConfiguration googleResourceConfiguration;

    @Autowired
    private StudyService studyService;

    @MockBean
    private SamClientService samService;

    @Before
    public void setup() throws Exception {
        billingProfileModel = connectedOperations.getOrCreateProfileForAccount(
            googleResourceConfiguration.getCoreBillingAccount());

        studySummaryModel = connectedOperations.createStudyWithFlight(
            billingProfileModel,
            "ingest-test-study.json");
        studyId = UUID.fromString(studySummaryModel.getId());
        study = studyService.retrieve(studyId);
        connectedOperations.stubOutSamCalls(samService);
    }
    @Test
    public void pathMiscTest() throws Exception {
        String result = fileDao.getDirectoryPath("/foo/bar/fribble");
        assertThat("Valid path", result, equalTo("/foo/bar"));

        result = fileDao.getDirectoryPath("/foo/bar");
        assertThat("Valid path", result, equalTo("/foo"));

        result = fileDao.getDirectoryPath("/foo");
        assertThat("Should be empty", result, equalTo(StringUtils.EMPTY));
    }

    @Test
    public void fileStateTest() throws Exception {
        FSFile fsFile = new FSFile()
            .mimeType(mimeType)
            .flightId(flightId)
            .studyId(studyId)
            .objectType(FSObjectType.INGESTING_FILE)
            .path(fileAPath)
            .description(description);

        Study study = studyService.retrieve(studyId);
        UUID fileAId = fileDao.createFileStart(study, fsFile);
        assertNotNull("File id not null", fileAId);

        FSFileInfo fsFileInfo = new FSFileInfo()
            .objectId(fileAId.toString())
            .studyId(studyId.toString())
            .createdDate(Instant.now().toString())
            .gspath("gs://mybucket/mystudy/myfile")
            .checksumCrc32c("myChecksum")
            .checksumMd5(null)
            .size(42L)
            .flightId(flightId);

        FSObjectBase typeObject = fileDao.createFileComplete(study, fsFileInfo);
        assertThat("Id matches", typeObject.getObjectId(), equalTo(fileAId));
        assertThat("Type is FILE", typeObject.getObjectType(), equalTo(FSObjectType.FILE));

        fileDao.createFileCompleteUndo(study, fileAId.toString());

        typeObject = fileDao.retrieveByIdNoThrow(study, typeObject.getObjectId());
        assertThat("Type is INGESTING_FILE", typeObject.getObjectType(),
            equalTo(FSObjectType.INGESTING_FILE));

        fileDao.createFileComplete(study, fsFileInfo);

        boolean exists = fileDao.deleteFileStart(study, fileAId.toString(), flightId);
        assertTrue("File exists - start", exists);
        exists = fileDao.deleteFileComplete(study, fileAId.toString(), flightId);
        assertTrue("File exists - complete", exists);
    }

    @Test
    public void deleteOnEmptyTest() throws Exception {
        FSFile fsObject = new FSFile()
            .studyId(studyId)
            .objectType(FSObjectType.INGESTING_FILE)
            .path(fileAPath)
            .mimeType(mimeType)
            .description(description)
            .flightId(flightId);

        UUID fileAId = fileDao.createFileStart(study, fsObject);
        assertNotNull("File id not null", fileAId);

        FSObjectBase topObject = getCheckPath(topPath, studyId, FSObjectType.DIRECTORY);
        FSObjectBase secondObject = getCheckPath(secondPath, studyId, FSObjectType.DIRECTORY);
        FSObjectBase thirdObject = getCheckPath(thirdPath, studyId, FSObjectType.DIRECTORY);
        FSObjectBase fileAObject = getCheckPath(fileAPath, studyId, FSObjectType.INGESTING_FILE);

        // Try to delete a directory with the deleteFile method; should fail
        try {
            fileDao.createFileStartUndo(study, secondObject.getPath(), flightId);
            fail("Should not have successfully deleted a directory");
        } catch (Exception ex) {
            assertTrue("Expected exception", ex instanceof FileSystemCorruptException);
            assertThat("Check expected error", ex.getMessage(), containsString("bad file object type"));
        }

        FSFileInfo fsFileInfo = makeFsFileInfo(fileAId.toString());
        fileDao.createFileComplete(study, fsFileInfo);

        boolean exists = fileDao.deleteFileStart(study, fileAId.toString(), flightId);
        assertTrue("File exists", exists);

        try {
            fileDao.deleteFileComplete(study, fileAId.toString(), "badFlightId");
        } catch (Exception ex) {
            assertTrue("Expected delete exception", ex instanceof InvalidFileSystemObjectTypeException);
            assertThat("Delete reason", ex.getMessage(), containsString("being deleted by someone else"));
        }

        try {
            fileDao.deleteFileComplete(study, secondObject.getObjectId().toString(), flightId);
        } catch (Exception ex) {
            assertTrue("Expected delete exception", ex instanceof InvalidFileSystemObjectTypeException);
            assertThat("Delete reason", ex.getMessage(), containsString("attempt to delete a directory"));
        }

        exists = fileDao.deleteFileComplete(study, fileAId.toString(), flightId);
        assertTrue("File existed", exists);

        // Now verify that the objects are gone.
        checkObjectGone(topObject);
        checkObjectGone(secondObject);
        checkObjectGone(thirdObject);
        checkObjectGone(fileAObject);
    }

    @Test
    public void dontDeleteOnNotEmptyTest() throws Exception {
        FSFile fsObject = new FSFile()
            .studyId(studyId)
            .objectType(FSObjectType.INGESTING_FILE)
            .path(fileAPath)
            .mimeType(mimeType)
            .description(description)
            .flightId(flightId);

        UUID fileAId = fileDao.createFileStart(study, fsObject);
        assertNotNull("File id not null", fileAId);

        FSObjectBase topObject = getCheckPath(topPath, studyId, FSObjectType.DIRECTORY);
        FSObjectBase secondObject = getCheckPath(secondPath, studyId, FSObjectType.DIRECTORY);
        FSObjectBase thirdObject = getCheckPath(thirdPath, studyId, FSObjectType.DIRECTORY);
        FSObjectBase fileAObject = getCheckPath(fileAPath, studyId, FSObjectType.INGESTING_FILE);

        fsObject.path(fileBPath);
        UUID fileBId = fileDao.createFileStart(study, fsObject);
        assertNotNull("File id not null", fileBId);

        FSObjectBase fileBObject = getCheckPath(fileBPath, studyId, FSObjectType.INGESTING_FILE);

        FSFileInfo fsFileInfo = makeFsFileInfo(fileAId.toString());
        fileDao.createFileComplete(study, fsFileInfo);
        fsFileInfo = makeFsFileInfo(fileBId.toString());
        fileDao.createFileComplete(study, fsFileInfo);


        boolean existed = fileDao.deleteFileStart(study, fileAId.toString(), flightId);
        assertTrue("File existed", existed);
        existed = fileDao.deleteFileComplete(study, fileAId.toString(), flightId);
        assertTrue("File existed", existed);

        // Directories and file B should still all be in place
        checkObjectPresent(topObject);
        checkObjectPresent(secondObject);
        checkObjectPresent(thirdObject);
        checkObjectPresent(fileBObject);
        checkObjectGone(fileAObject);

        // Don't delete file with dependencies
        addDatasetDependency(fileBId);
        addDatasetDependency(fileBId);
        addDatasetDependency(fileBId);

        try {
            fileDao.deleteFileStart(study, fileBId.toString(), flightId);
            fail("Should not have successfully deleted");
        } catch (Exception ex) {
            assertTrue("Correct dependency exception", ex instanceof FileSystemObjectDependencyException);
            assertThat("Correct message", ex.getMessage(), containsString("dataset"));
        }

        removeDatasetDependency(fileBId);
        removeDatasetDependency(fileBId);

        try {
            fileDao.deleteFileStart(study, fileBId.toString(), flightId);
            fail("Should not have successfully deleted");
        } catch (Exception ex) {
            assertTrue("Correct dependency exception", ex instanceof FileSystemObjectDependencyException);
            assertThat("Correct message", ex.getMessage(), containsString("dataset"));
        }

        removeDatasetDependency(fileBId);

        fileDao.deleteFileStart(study, fileBId.toString(), flightId);

        addDatasetDependency(fileBId);

        try {
            fileDao.deleteFileComplete(study, fileBId.toString(), flightId);
            fail("Should not have successfully deleted");
        } catch (Exception ex) {
            assertTrue("Correct dependency exception", ex instanceof FileSystemCorruptException);
            assertThat("Correct message", ex.getMessage(), containsString("any references"));
        }

        removeDatasetDependency(fileBId);

        existed = fileDao.deleteFileComplete(study, fileBId.toString(), flightId);
        assertTrue("File B existed", existed);

        // Now verify that the objects are gone.
        checkObjectGone(topObject);
        checkObjectGone(secondObject);
        checkObjectGone(thirdObject);
        checkObjectGone(fileAObject);
        checkObjectGone(fileBObject);
    }

    // Note: this test does logging because it runs for several minutes. Travis gets impatient if it hasn't seen
    // output for 10 minutes. Sometimes if things are slow, this test can take longer than that.
    @Test
    public void testStudyDelete() throws Exception {
        // Make 1001 files and then delete the study
        FSFile fsObject = new FSFile()
            .studyId(studyId)
            .objectType(FSObjectType.INGESTING_FILE)
            .mimeType(mimeType)
            .description(description)
            .flightId(flightId);

        FSFileInfo fsFileInfo = makeFsFileInfo("later");

        logger.info("testStudyDelete:Creating:");
        for (int i = 1; i <= 1001; i++) {
            fsObject.path("/file_" + i);
            UUID objectId = fileDao.createFileStart(study, fsObject);
            fsFileInfo.objectId(objectId.toString());
            fileDao.createFileComplete(study, fsFileInfo);
            if (i % 100 == 0) {
                logger.info(".." + i);
            }
        }

        // Make sure some of the files are there.
        logger.info("testStudyDelete:Reading:");
        for (int i = 1; i <= 1001; i++) {
            FSObjectBase testObject = fileDao.retrieveByPathNoThrow(study, "/file_" + i);
            assertNotNull(testObject);
            if (i % 100 == 0) {
                logger.info(".." + i);
            }
        }

        logger.info("testStudyDelete:Deleting...");
        fileDao.deleteFilesFromStudy(study);

        // Make sure they are all gone
        logger.info("testStudyDelete:Check deleted:");
        for (int i = 1; i <= 1001; i++) {
            FSObjectBase noObject = fileDao.retrieveByPathNoThrow(study, "/file_" + i);
            assertNull("Object is deleted", noObject);
            if (i % 100 == 0) {
                logger.info(".." + i);
            }
        }
        logger.info("testStudyDelete:Done");
    }

    @Test
    public void pathLookupTest() throws Exception {
        FSFile fsObject = new FSFile()
            .studyId(studyId)
            .objectType(FSObjectType.INGESTING_FILE)
            .path(fileAPath)
            .mimeType(mimeType)
            .description(description)
            .flightId(flightId);

        UUID fileAId = fileDao.createFileStart(study, fsObject);
        FSFileInfo fsFileInfo = makeFsFileInfo(fileAId.toString());
        fileDao.createFileComplete(study, fsFileInfo);

        FSObjectBase fileAObject = fileDao.retrieve(study, fileAId);
        FSObjectBase testObject = fileDao.retrieveByPath(study, fileAPath);
        assertThat("Path lookup matched fileid lookup", fileAObject, equalTo(testObject));

        try {
            fileDao.retrieveByPath(study, thirdPath + "/not-there");
            fail("Should not have successfully retrieved");
        } catch (Exception ex) {
            assertTrue("Correct path not found exception", ex instanceof FileSystemObjectNotFoundException);
            assertThat("Correct message", ex.getMessage(), containsString("Object not found"));
        }

        FSObjectBase dirObject = fileDao.retrieveWithContentsByPath(study, "/");
        assertThat("Root is directory", dirObject.getObjectType(), equalTo(FSObjectType.DIRECTORY));
        FSDir enumDir = (FSDir)dirObject;
        assertThat("Root has contents", enumDir.getContents().size(), equalTo(1));
        FSObjectBase nextDirObject = enumDir.getContents().get(0);
        assertThat("Top is directory", nextDirObject.getObjectType(), equalTo(FSObjectType.DIRECTORY));

        String path = nextDirObject.getPath(); // /top
        dirObject = fileDao.retrieveWithContentsByPath(study, path);
        assertThat("Top is directory", dirObject.getObjectType(), equalTo(FSObjectType.DIRECTORY));
        enumDir = (FSDir)dirObject;
        assertThat("Top has contents", enumDir.getContents().size(), equalTo(1));
        nextDirObject = enumDir.getContents().get(0);
        assertThat("Second is directory", nextDirObject.getObjectType(), equalTo(FSObjectType.DIRECTORY));

        path = nextDirObject.getPath(); // /top/second
        dirObject = fileDao.retrieveWithContentsByPath(study, path);
        assertThat("Second is directory", dirObject.getObjectType(), equalTo(FSObjectType.DIRECTORY));
        enumDir = (FSDir)dirObject;
        assertThat("Second has contents", enumDir.getContents().size(), equalTo(1));
        nextDirObject = enumDir.getContents().get(0);
        assertThat("Third is a directory", nextDirObject.getObjectType(), equalTo(FSObjectType.DIRECTORY));

        path = nextDirObject.getPath(); // /top/second/third
        dirObject = fileDao.retrieveWithContentsByPath(study, path);
        assertThat("Third is directory", dirObject.getObjectType(), equalTo(FSObjectType.DIRECTORY));
        enumDir = (FSDir)dirObject;
        assertThat("Third has contents", enumDir.getContents().size(), equalTo(1));
        nextDirObject = enumDir.getContents().get(0);
        assertThat("Fourth is a file", nextDirObject.getObjectType(), equalTo(FSObjectType.FILE));

        boolean existed = fileDao.deleteFileStart(study, fileAId.toString(), flightId);
        assertTrue("File existed", existed);
        existed = fileDao.deleteFileComplete(study, fileAId.toString(), flightId);
        assertTrue("File existed", existed);
    }


    private void addDatasetDependency(UUID objectId) {
        dependencyDao.storeDatasetFileDependency(study, datasetId, objectId.toString());
    }

    private void removeDatasetDependency(UUID objectId) {
        dependencyDao.removeDatasetFileDependency(study, datasetId, objectId.toString());
    }

    private void checkObjectPresent(FSObjectBase fsObject) {
        FSObjectBase thereObject = fileDao.retrieveByPathNoThrow(study, fsObject.getPath());
        assertNotNull("Object is there by path", thereObject);
        thereObject = fileDao.retrieveByIdNoThrow(study, fsObject.getObjectId());
        assertNotNull("Object is there by id", thereObject);
    }


    private void checkObjectGone(FSObjectBase fsObject) {
        FSObjectBase goneObject = fileDao.retrieveByPathNoThrow(study, fsObject.getPath());
        assertNull("Object is gone by path", goneObject);
        goneObject = fileDao.retrieveByIdNoThrow(study, fsObject.getObjectId());
        assertNull("Object is gone by id", goneObject);
    }

    private FSObjectBase getCheckPath(String path, UUID studyId, FSObjectType objectType) {
        FSObjectBase fsObject = fileDao.retrieveByPathNoThrow(study, path);
        assertNotNull("Object not null", fsObject);
        assertThat("Object has correct path", fsObject.getPath(), equalTo(path));
        assertThat("Correct study", fsObject.getStudyId(), equalTo(studyId));
        assertThat("Correct object type", fsObject.getObjectType(), equalTo(objectType));
        if (objectType != FSObjectType.DIRECTORY) { // Only filled in for files
            FSFile fsFile = (FSFile)fsObject;
            assertThat("Correct mime type", mimeType, equalTo(fsFile.getMimeType()));
            assertThat("Correct description", description, equalTo(fsFile.getDescription()));
        }
        return fsObject;
    }

    private FSFileInfo makeFsFileInfo(String objectId) {
        return new FSFileInfo()
            .objectId(objectId)
            .studyId(studyId.toString())
            .createdDate(Instant.now().toString())
            .gspath("gs://mybucket/mystudy/myfile")
            .checksumCrc32c("myChecksum")
            .checksumMd5(null)
            .size(42L)
            .flightId(flightId);
    }

}
