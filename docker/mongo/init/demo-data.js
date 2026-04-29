const hotelbookingDb = db.getSiblingDB("hotelbooking");

/*
 * Spring Data Mongo in this project stores java.util.UUID as BSON Binary subtype 03.
 * The base64 values below are Java legacy UUID binary representations.
 */
function javaLegacyUuid(uuidText, base64Value) {
  return BinData(3, base64Value);
}

/*
 * Spring Data stores LocalDate as BSON Date.
 * In your local environment 2030-06-01 is stored as 2030-05-31T21:00:00.000Z.
 */
function localDate(dateString) {
  const parts = dateString.split("-");
  const year = Number(parts[0]);
  const month = Number(parts[1]) - 1;
  const day = Number(parts[2]);

  return new Date(Date.UTC(year, month, day, 0, 0, 0, 0) - 3 * 60 * 60 * 1000);
}

function dateOnlyString(date) {
  return date.toISOString().substring(0, 10);
}

const KAZAN_HOTEL_ID_TEXT = "10000000-0000-0000-0000-000000000001";
const ALMETYEVSK_HOTEL_ID_TEXT = "10000000-0000-0000-0000-000000000002";
const CHELNY_HOTEL_ID_TEXT = "10000000-0000-0000-0000-000000000003";

const KAZAN_STANDARD_ID_TEXT = "20000000-0000-0000-0000-000000000001";
const KAZAN_FAMILY_ID_TEXT = "20000000-0000-0000-0000-000000000002";
const KAZAN_LUX_ID_TEXT = "20000000-0000-0000-0000-000000000003";

const ALMETYEVSK_STANDARD_ID_TEXT = "20000000-0000-0000-0000-000000000004";
const ALMETYEVSK_BUSINESS_ID_TEXT = "20000000-0000-0000-0000-000000000005";

const CHELNY_ECONOMY_ID_TEXT = "20000000-0000-0000-0000-000000000006";
const CHELNY_STANDARD_ID_TEXT = "20000000-0000-0000-0000-000000000007";

const KAZAN_HOTEL_ID = javaLegacyUuid(KAZAN_HOTEL_ID_TEXT, "AAAAAAAAABABAAAAAAAAAA==");
const ALMETYEVSK_HOTEL_ID = javaLegacyUuid(ALMETYEVSK_HOTEL_ID_TEXT, "AAAAAAAAABACAAAAAAAAAA==");
const CHELNY_HOTEL_ID = javaLegacyUuid(CHELNY_HOTEL_ID_TEXT, "AAAAAAAAABADAAAAAAAAAA==");

const KAZAN_STANDARD_ID = javaLegacyUuid(KAZAN_STANDARD_ID_TEXT, "AAAAAAAAACABAAAAAAAAAA==");
const KAZAN_FAMILY_ID = javaLegacyUuid(KAZAN_FAMILY_ID_TEXT, "AAAAAAAAACACAAAAAAAAAA==");
const KAZAN_LUX_ID = javaLegacyUuid(KAZAN_LUX_ID_TEXT, "AAAAAAAAACADAAAAAAAAAA==");

const ALMETYEVSK_STANDARD_ID =
  javaLegacyUuid(ALMETYEVSK_STANDARD_ID_TEXT, "AAAAAAAAACAEAAAAAAAAAA==");
const ALMETYEVSK_BUSINESS_ID =
  javaLegacyUuid(ALMETYEVSK_BUSINESS_ID_TEXT, "AAAAAAAAACAFAAAAAAAAAA==");

const CHELNY_ECONOMY_ID = javaLegacyUuid(CHELNY_ECONOMY_ID_TEXT, "AAAAAAAAACAGAAAAAAAAAA==");
const CHELNY_STANDARD_ID = javaLegacyUuid(CHELNY_STANDARD_ID_TEXT, "AAAAAAAAACAHAAAAAAAAAA==");

const AVAILABILITY_FROM = "2030-06-01";
const AVAILABILITY_TO = "2030-06-30";

const hotels = [
  {
    _id: KAZAN_HOTEL_ID,
    name: "Demo Kazan Hotel",
    city: "Kazan",
    roomTypes: [
      {
        roomTypeId: KAZAN_STANDARD_ID,
        name: "STANDARD",
        guestCapacity: 2
      },
      {
        roomTypeId: KAZAN_FAMILY_ID,
        name: "FAMILY",
        guestCapacity: 4
      },
      {
        roomTypeId: KAZAN_LUX_ID,
        name: "LUX",
        guestCapacity: 4
      }
    ]
  },
  {
    _id: ALMETYEVSK_HOTEL_ID,
    name: "Demo Almetyevsk Hotel",
    city: "Almetyevsk",
    roomTypes: [
      {
        roomTypeId: ALMETYEVSK_STANDARD_ID,
        name: "STANDARD",
        guestCapacity: 2
      },
      {
        roomTypeId: ALMETYEVSK_BUSINESS_ID,
        name: "BUSINESS",
        guestCapacity: 2
      }
    ]
  },
  {
    _id: CHELNY_HOTEL_ID,
    name: "Demo Naberezhnye Chelny Hotel",
    city: "Naberezhnye Chelny",
    roomTypes: [
      {
        roomTypeId: CHELNY_ECONOMY_ID,
        name: "ECONOMY",
        guestCapacity: 1
      },
      {
        roomTypeId: CHELNY_STANDARD_ID,
        name: "STANDARD",
        guestCapacity: 2
      }
    ]
  }
];

const availabilityDefinitions = [
  {
    hotelIdText: KAZAN_HOTEL_ID_TEXT,
    roomTypeIdText: KAZAN_STANDARD_ID_TEXT,
    hotelId: KAZAN_HOTEL_ID,
    roomTypeId: KAZAN_STANDARD_ID,
    totalRooms: 6
  },
  {
    hotelIdText: KAZAN_HOTEL_ID_TEXT,
    roomTypeIdText: KAZAN_FAMILY_ID_TEXT,
    hotelId: KAZAN_HOTEL_ID,
    roomTypeId: KAZAN_FAMILY_ID,
    totalRooms: 3
  },
  {
    hotelIdText: KAZAN_HOTEL_ID_TEXT,
    roomTypeIdText: KAZAN_LUX_ID_TEXT,
    hotelId: KAZAN_HOTEL_ID,
    roomTypeId: KAZAN_LUX_ID,
    totalRooms: 2
  },
  {
    hotelIdText: ALMETYEVSK_HOTEL_ID_TEXT,
    roomTypeIdText: ALMETYEVSK_STANDARD_ID_TEXT,
    hotelId: ALMETYEVSK_HOTEL_ID,
    roomTypeId: ALMETYEVSK_STANDARD_ID,
    totalRooms: 4
  },
  {
    hotelIdText: ALMETYEVSK_HOTEL_ID_TEXT,
    roomTypeIdText: ALMETYEVSK_BUSINESS_ID_TEXT,
    hotelId: ALMETYEVSK_HOTEL_ID,
    roomTypeId: ALMETYEVSK_BUSINESS_ID,
    totalRooms: 2
  },
  {
    hotelIdText: CHELNY_HOTEL_ID_TEXT,
    roomTypeIdText: CHELNY_ECONOMY_ID_TEXT,
    hotelId: CHELNY_HOTEL_ID,
    roomTypeId: CHELNY_ECONOMY_ID,
    totalRooms: 5
  },
  {
    hotelIdText: CHELNY_HOTEL_ID_TEXT,
    roomTypeIdText: CHELNY_STANDARD_ID_TEXT,
    hotelId: CHELNY_HOTEL_ID,
    roomTypeId: CHELNY_STANDARD_ID,
    totalRooms: 5
  }
];

function upsertHotels() {
  for (const hotel of hotels) {
    hotelbookingDb.hotels.replaceOne(
      { _id: hotel._id },
      hotel,
      { upsert: true }
    );
  }
}

function upsertAvailability() {
  const from = localDate(AVAILABILITY_FROM);
  const to = localDate(AVAILABILITY_TO);

  for (const definition of availabilityDefinitions) {
    for (
      let current = new Date(from);
      current <= to;
      current.setUTCDate(current.getUTCDate() + 1)
    ) {
      const date = new Date(current);
      const logicalDate = new Date(date.getTime() + 3 * 60 * 60 * 1000);
      const dateString = dateOnlyString(logicalDate);

      const availabilityId =
        definition.hotelIdText + ":" + definition.roomTypeIdText + ":" + dateString;

      const availability = {
        _id: availabilityId,
        hotelId: definition.hotelId,
        roomTypeId: definition.roomTypeId,
        date: date,
        totalRooms: definition.totalRooms,
        heldRooms: 0,
        bookedRooms: 0
      };

      hotelbookingDb.room_availability.replaceOne(
        { _id: availabilityId },
        availability,
        { upsert: true }
      );
    }
  }
}

function clearDemoHolds() {
  hotelbookingDb.room_holds.deleteMany({
    hotelId: {
      $in: [KAZAN_HOTEL_ID, ALMETYEVSK_HOTEL_ID, CHELNY_HOTEL_ID]
    }
  });
}

function printSummary() {
  print("Demo inventory data has been initialized in Java/Spring Data Mongo format.");
  print("Availability range: " + AVAILABILITY_FROM + " .. " + AVAILABILITY_TO);
  print("Hotel ids:");
  print("Demo Kazan Hotel: " + KAZAN_HOTEL_ID_TEXT);
  print("Demo Almetyevsk Hotel: " + ALMETYEVSK_HOTEL_ID_TEXT);
  print("Demo Naberezhnye Chelny Hotel: " + CHELNY_HOTEL_ID_TEXT);
  print("Room type ids:");
  print("Kazan STANDARD: " + KAZAN_STANDARD_ID_TEXT);
  print("Kazan FAMILY: " + KAZAN_FAMILY_ID_TEXT);
  print("Kazan LUX: " + KAZAN_LUX_ID_TEXT);
  print("Almetyevsk STANDARD: " + ALMETYEVSK_STANDARD_ID_TEXT);
  print("Almetyevsk BUSINESS: " + ALMETYEVSK_BUSINESS_ID_TEXT);
  print("Chelny ECONOMY: " + CHELNY_ECONOMY_ID_TEXT);
  print("Chelny STANDARD: " + CHELNY_STANDARD_ID_TEXT);
}

upsertHotels();
upsertAvailability();
clearDemoHolds();
printSummary();