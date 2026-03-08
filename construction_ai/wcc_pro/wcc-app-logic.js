// wcc-app-logic.js
module.exports = {
  createProject(clientId, estimateTemplate) {
    return {
      clientId,
      tasks: this.loadTasksFromTemplate(estimateTemplate),
      createdAt: Date.now(),
      status: "active"
    };
  },
  loadTasksFromTemplate(template) {
    const templates = {
      "bathroom remodel": [
        { task: "demo existing bath", price: 450 },
        { task: "install backer board", price: 300 },
        { task: "tile floor", price: 700 }
      ]
    };
    return templates[template] || [];
  }
};
