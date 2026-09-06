package com.omnisocials.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.omnisocials.OmniSocials;

/**
 * Approval workflows, configured in the OmniSocials dashboard (Approvals).
 * Accessed via {@code client.approvalWorkflows()}.
 *
 * <p>Route a post through a workflow at create time by passing
 * {@code approval_workflow_id} (a workflow's {@code id}) to
 * {@link PostsResource#create(java.util.Map)}.
 */
public final class ApprovalWorkflowsResource extends ApiResource {

  public ApprovalWorkflowsResource(OmniSocials client) {
    super(client);
  }

  /**
   * {@code GET /approval-workflows} - the workflows this workspace can use
   * (company-wide plus workspace-bound), with steps and named approvers.
   */
  public JsonNode list() {
    return client.get("/approval-workflows");
  }
}
