package com.emc.ecs.servicebroker.service;

import com.emc.ecs.servicebroker.model.PlanProxy;
import com.emc.ecs.servicebroker.model.ReclaimPolicy;
import com.emc.ecs.servicebroker.model.ServiceDefinitionProxy;
import com.emc.ecs.servicebroker.repository.ServiceInstance;
import com.emc.ecs.servicebroker.repository.ServiceInstanceRepository;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.emc.ecs.common.Fixtures.*;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Ginkgo4jRunner.class)
public class BucketInstanceWorkflowTest {

    private EcsService ecs;
    private ServiceInstanceRepository instanceRepo;
    private InstanceWorkflow workflow;
    private final Map<String, Object> parameters = new HashMap<>();
    private final ServiceDefinitionProxy serviceProxy = new ServiceDefinitionProxy();
    private final PlanProxy planProxy = new PlanProxy();
    private final ServiceInstance bucketInstance = serviceInstanceFixture();
    private final ArgumentCaptor<ServiceInstance> instCaptor = ArgumentCaptor.forClass(ServiceInstance.class);

    {
        Describe("BucketInstanceWorkflow", () -> {
            BeforeEach(() -> {
                ecs = mock(EcsService.class);
                instanceRepo = mock(ServiceInstanceRepository.class);
                workflow = new BucketInstanceWorkflow(instanceRepo, ecs);

                when(ecs.wipeAndDeleteBucket(any())).thenReturn(CompletableFuture.completedFuture(true));
            });

            Context("#changePlan", () -> {
                BeforeEach(() -> when(ecs.changeBucketPlan(BUCKET_NAME, serviceProxy, planProxy, parameters))
                        .thenReturn(new HashMap<>()));

                It("should change the plan", () -> {
                    workflow.changePlan(BUCKET_NAME, serviceProxy, planProxy, parameters);
                    verify(ecs, times(1))
                            .changeBucketPlan(BUCKET_NAME, serviceProxy, planProxy, parameters);
                });
            });

            Context("#delete with ReclaimPolicy", () -> {

                BeforeEach(() -> {
                    doNothing().when(instanceRepo).save(any(ServiceInstance.class));
                    when(instanceRepo.find(BUCKET_NAME)).thenReturn(bucketInstance);
                });

                Context("with no ReclaimPolicy", () -> {
                    It("should call delete and NOT wipe bucket", () -> {
                        CompletableFuture result = workflow.delete(BUCKET_NAME);
                        assertNull(result);
                        verify(ecs, times(1)).deleteBucket(BUCKET_NAME);
                        verify(ecs, times(0)).wipeAndDeleteBucket(BUCKET_NAME);
                    });
                });

                Context("with Fail ReclaimPolicy", () -> {
                    It("should call delete and NOT wipe bucket", () -> {
                        bucketInstance.setServiceSettings(Collections.singletonMap(RECLAIM_POLICY, ReclaimPolicy.Fail));

                        CompletableFuture result = workflow.delete(BUCKET_NAME);
                        assertNull(result);
                        verify(ecs, times(1)).deleteBucket(BUCKET_NAME);
                        verify(ecs, times(0)).wipeAndDeleteBucket(BUCKET_NAME);
                    });
                });

                Context("with Detach ReclaimPolicy", () -> {
                    It("should not call delete", () -> {
                        bucketInstance.setServiceSettings(Collections.singletonMap(RECLAIM_POLICY, ReclaimPolicy.Detach));

                        CompletableFuture result = workflow.delete(BUCKET_NAME);
                        assertNull(result);
                        verify(ecs, times(0)).deleteBucket(BUCKET_NAME);
                        verify(ecs, times(0)).wipeAndDeleteBucket(BUCKET_NAME);
                    });
                });

                Context("with Delete ReclaimPolicy", () -> {
                    It("should wipe and delete", () -> {
                        bucketInstance.setServiceSettings(Collections.singletonMap(RECLAIM_POLICY, ReclaimPolicy.Delete));

                        CompletableFuture result = workflow.delete(BUCKET_NAME);
                        assertNotNull(result);
                        verify(ecs, times(0)).deleteBucket(BUCKET_NAME);
                        verify(ecs, times(1)).wipeAndDeleteBucket(BUCKET_NAME);
                    });
                });
            });

            Context("#delete", () -> {

                BeforeEach(() -> doNothing().when(instanceRepo)
                        .save(any(ServiceInstance.class)));

                Context("with multiple references", () -> {
                    BeforeEach(() -> {
                        Set<String> refs = new HashSet<>(Arrays.asList(
                                BUCKET_NAME,
                                BUCKET_NAME + "2"
                        ));
                        bucketInstance.setReferences(refs);
                        when(instanceRepo.find(BUCKET_NAME))
                                .thenReturn(bucketInstance);
                        when(instanceRepo.find(BUCKET_NAME + "2"))
                                .thenReturn(bucketInstance);
                    });

                    Context("the bucket is included in references", () -> {
                        It("should not delete the bucket", () -> {
                            workflow.delete(BUCKET_NAME);
                            verify(ecs, times(0))
                                    .deleteBucket(BUCKET_NAME);
                        });

                        It("should update each references", () -> {
                            workflow.delete(BUCKET_NAME);
                            verify(instanceRepo, times(1))
                                    .save(instCaptor.capture());
                            ServiceInstance savedInst = instCaptor.getValue();
                            assertEquals(1, savedInst.getReferenceCount());
                            assert (savedInst.getReferences().contains(BUCKET_NAME + "2"));
                        });
                    });

                });

                Context("with a single reference", () -> {
                    BeforeEach(() -> {
                        Set<String> refs = new HashSet<>(Collections.singletonList(BUCKET_NAME));
                        bucketInstance.setReferences(refs);
                        when(instanceRepo.find(BUCKET_NAME))
                                .thenReturn(bucketInstance);
                        when(ecs.deleteBucket(BUCKET_NAME)).thenReturn(null);
                    });

                    It("should delete the bucket", () -> {
                        workflow.delete(BUCKET_NAME);
                        verify(ecs, times(1))
                                .deleteBucket(BUCKET_NAME);
                    });
                });
            });

            Context("#create", () -> {
                BeforeEach(() -> {
                    when(ecs.createBucket(BUCKET_NAME, serviceProxy, planProxy, parameters))
                            .thenReturn(new HashMap<>());
                    workflow.withCreateRequest(bucketCreateRequestFixture(parameters));
                });

                It("should create the bucket", () -> {
                    workflow.create(BUCKET_NAME, serviceProxy, planProxy, parameters);
                    verify(ecs, times(1))
                            .createBucket(BUCKET_NAME, serviceProxy, planProxy, parameters);
                });

                It("should return the service instance", () -> {
                    ServiceInstance instance = workflow.create(BUCKET_NAME, serviceProxy, planProxy, parameters);
                    assertEquals(BUCKET_NAME, instance.getName());
                    assertEquals(BUCKET_NAME, instance.getServiceInstanceId());
                });
            });
        });
    }
}
