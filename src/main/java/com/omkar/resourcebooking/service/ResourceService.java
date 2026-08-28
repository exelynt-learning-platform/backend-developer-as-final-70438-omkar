package com.omkar.resourcebooking.service;

import com.omkar.resourcebooking.dto.ResourceRequest;
import com.omkar.resourcebooking.dto.ResourceResponse;
import com.omkar.resourcebooking.entity.Resource;
import com.omkar.resourcebooking.exception.ResourceNotFoundException;
import com.omkar.resourcebooking.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = new Resource(
                request.getName(),
                request.getDescription(),
                request.getType(),
                request.isAvailable(),
                request.getPrice()
        );
        Resource saved = resourceRepository.save(resource);
        return mapToResponse(saved);
    }

    public List<ResourceResponse> getAllResources() {
        return resourceRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ResourceResponse getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
        return mapToResponse(resource);
    }

    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));

        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setAvailable(request.isAvailable());
        resource.setPrice(request.getPrice());

        Resource updated = resourceRepository.save(resource);
        return mapToResponse(updated);
    }

    public void deleteResource(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
        resourceRepository.delete(resource);
    }

    private ResourceResponse mapToResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.isAvailable(),
                resource.getPrice()
        );
    }
}
