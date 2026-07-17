const { onValueCreated, onValueUpdated } = require("firebase-functions/v2/database");
const admin = require("firebase-admin");
const { onSchedule } = require("firebase-functions/v2/scheduler");

admin.initializeApp();
async function saveNotification(companyKey, data) {
  const ref = admin.database()
    .ref(`Companies/${companyKey}/notifications`)
    .push();

  await ref.set({
    ...data,
    delivered: false,
    createdAt: admin.database.ServerValue.TIMESTAMP
  });
}


/**
 * 🔔 CHECK-IN NOTIFICATION
 */
exports.notifyAdminOnCheckIn = onValueCreated(
  "/Companies/{companyKey}/attendance/{date}/{employeeMobile}/checkInTime",
  async (event) => {

    const checkInTime = event.data.val();
    if (!checkInTime) return;

    const { companyKey, employeeMobile } = event.params;

    const infoSnap = await admin.database()
      .ref(`Companies/${companyKey}/companyInfo`)
      .once("value");

    const info = infoSnap.val();
    if (!info || info.notifyAttendance === false || !info.adminFcmToken) return;

    const empSnap = await admin.database()
      .ref(`Companies/${companyKey}/employees/${employeeMobile}/info/employeeName`)
      .once("value");

    const employeeName = empSnap.val() || employeeMobile;

    await saveNotification(companyKey, {
  title: "Attendance Check-In",
  body: `${employeeName} checked in at ${checkInTime}`,
  type: "ATTENDANCE_CHECK_IN",
  target: "ADMIN"
});




    await admin.messaging().send({
      token: info.adminFcmToken,
      notification: {
        title: "Attendance Check-In",
        body: `${employeeName} checked in at ${checkInTime}`
      }
    });
  }
);

/**
 * 🔔 CHECK-OUT NOTIFICATION
 */
exports.notifyAdminOnCheckOut = onValueCreated(
  "/Companies/{companyKey}/attendance/{date}/{employeeMobile}/checkOutTime",
  async (event) => {

    const checkOutTime = event.data.val();
    if (!checkOutTime) return;

    const { companyKey, employeeMobile, date } = event.params;

    const infoSnap = await admin.database()
      .ref(`Companies/${companyKey}/companyInfo`)
      .once("value");

    const info = infoSnap.val();
    if (!info || info.notifyAttendance === false || !info.adminFcmToken) return;

    const empSnap = await admin.database()
      .ref(`Companies/${companyKey}/employees/${employeeMobile}/info/employeeName`)
      .once("value");

    const employeeName = empSnap.val() || employeeMobile;

    const attSnap = await admin.database()
      .ref(`Companies/${companyKey}/attendance/${date}/${employeeMobile}`)
      .once("value");

    const att = attSnap.val() || {};

    await saveNotification(companyKey, {
  title: "Attendance Check-Out",
  body: `${employeeName} checked out at ${checkOutTime} (Total: ${att.totalHours || "0"}h)`,
  type: "ATTENDANCE_CHECK_OUT",
  target: "ADMIN"
});


    await admin.messaging().send({
      token: info.adminFcmToken,
      notification: {
        title: "Attendance Check-Out",
        body: `${employeeName} checked out at ${checkOutTime} (Total: ${att.totalHours || "0"}h)`
      }
    });
  }
);

/**
 * 🔔 EMPLOYEE LEAVE STATUS CHANGE
 */
exports.notifyEmployeeOnLeaveStatusChange = onValueUpdated(
  "/Companies/{companyKey}/leaves/{leaveId}/status",
  async (event) => {

    const before = event.data.before.val();
    const after = event.data.after.val();
    if (before === after) return;

    const { companyKey, leaveId } = event.params;

    const leaveSnap = await admin.database()
      .ref(`Companies/${companyKey}/leaves/${leaveId}`)
      .once("value");

    const leave = leaveSnap.val();
    if (!leave || !leave.employeeMobile) return;

    const tokenSnap = await admin.database()
      .ref(`Companies/${companyKey}/employees/${leave.employeeMobile}/info/fcmToken`)
      .once("value");

    const fcmToken = tokenSnap.val();
    if (!fcmToken) return;

    let body = "";
    if (after === "APPROVED") {
      body = `✅ Your leave (${leave.fromDate} to ${leave.toDate}) has been approved.`;
    } else if (after === "REJECTED") {
      body = `❌ Your leave (${leave.fromDate} to ${leave.toDate}) was rejected.`;
    } else {
      return;
    }

    await saveNotification(companyKey, {
  title: "Leave Update",
  body,
  type: "LEAVE_STATUS",
  target: "EMPLOYEE",
  employeeMobile: leave.employeeMobile
});


    await admin.messaging().send({
      token: fcmToken,
      notification: {
        title: "Leave Update",
        body
      },
      data: {
        type: "LEAVE_STATUS",
        status: after
      }
    });
  }
);

/**
 * 🔔 ADMIN – NEW LEAVE APPLIED
 */
exports.notifyAdminOnLeaveApplied = onValueCreated(
  "/Companies/{companyKey}/leaves/{leaveId}",
  async (event) => {

    const leave = event.data.val();
    if (!leave || !leave.employeeMobile) return;

    const { companyKey, leaveId } = event.params;

    const infoSnap = await admin.database()
      .ref(`Companies/${companyKey}/companyInfo`)
      .once("value");

    const info = infoSnap.val();
    if (!info || !info.adminFcmToken) return;

    const name = leave.employeeName || leave.employeeMobile;
await saveNotification(companyKey, {
  title: "New Leave Request",
  body: `${name} applied leave (${leave.fromDate} → ${leave.toDate})`,
  type: "LEAVE_APPLIED",
  target: "ADMIN",
  leaveId
});

    await admin.messaging().send({
      token: info.adminFcmToken,
      notification: {
        title: "New Leave Request",
        body: `${name} applied leave (${leave.fromDate} → ${leave.toDate})`
      },
      data: {
        type: "LEAVE_APPLIED",
        leaveId
      }
    });
  }
);/**
   * 🔔 AUTO MARK ABSENT
   * Runs every day at 12:10 AM (Asia/Kolkata)
   */
  exports.markAbsentEmployees = onSchedule(
    {
      schedule: "10 0 * * *", // 12:10 AM every day
      timeZone: "Asia/Kolkata",
      region: "us-central1",
    },
    async () => {
      try {
        const db = admin.database();

        // Get yesterday's date in India timezone
        const now = new Date();
        const indiaNow = new Date(
          now.toLocaleString("en-US", {
            timeZone: "Asia/Kolkata",
          })
        );

        indiaNow.setDate(indiaNow.getDate() - 1);

        const attendanceDate = indiaNow
          .toISOString()
          .split("T")[0];

        console.log(
          `Checking attendance for ${attendanceDate}`
        );

        const companiesSnap = await db
          .ref("Companies")
          .once("value");

        if (!companiesSnap.exists()) {
          console.log("No companies found.");
          return;
        }

        const companies = companiesSnap.val();

        for (const companyKey of Object.keys(companies)) {

          const attendanceRef = db.ref(
            `Companies/${companyKey}/attendance/${attendanceDate}`
          );

          const attendanceSnap = await attendanceRef.once("value");

          if (!attendanceSnap.exists()) {
            continue;
          }

          const employees = attendanceSnap.val();

          for (const employeeMobile of Object.keys(employees)) {

            const employee = employees[employeeMobile];

            const hasCheckIn =
              employee.checkInTime &&
              employee.checkInTime.trim() !== "";

            const hasCheckOut =
              employee.checkOutTime &&
              employee.checkOutTime.trim() !== "";

            const alreadyAbsent =
              employee.finalStatus === "Absent";

            if (
              hasCheckIn &&
              !hasCheckOut &&
              !alreadyAbsent
            ) {

              await attendanceRef
                .child(employeeMobile)
                .update({
                  status: "Absent",
                  finalStatus: "Absent",
                  totalMinutes: 0,
                  totalHours: "0",
                });

              console.log(
                `Marked Absent -> Company: ${companyKey}, Employee: ${employeeMobile}`
              );
            }
          }
        }

        console.log("Auto absent process completed successfully.");

      } catch (error) {
        console.error(
          "Auto absent scheduler failed:",
          error
        );
      }
    }
  );

