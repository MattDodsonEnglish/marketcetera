//
// this file is automatically generated
//
package org.marketcetera.strategy;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;
import org.marketcetera.admin.User;
import org.marketcetera.admin.dao.UserDao;
import org.marketcetera.admin.provisioning.ProvisioningAgent;
import org.marketcetera.admin.user.PersistentUser;
import org.marketcetera.cluster.ClusterData;
import org.marketcetera.cluster.service.ClusterService;
import org.marketcetera.core.PlatformServices;
import org.marketcetera.core.Preserve;
import org.marketcetera.core.file.DirectoryWatcherImpl;
import org.marketcetera.core.file.DirectoryWatcherSubscriber;
import org.marketcetera.strategy.dao.PersistentStrategyInstance;
import org.marketcetera.strategy.dao.StrategyInstanceDao;
import org.marketcetera.util.log.SLF4JLoggerProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

/* $License$ */

/**
 * Provides StrategyServiceImpl services.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
@Preserve
@Component
@AutoConfiguration
public class StrategyServiceImpl
        implements StrategyService,DirectoryWatcherSubscriber
{
    /**
     * Validate and start the object.
     */
    @PostConstruct
    public void start()
    {
        serviceName = PlatformServices.getServiceName(getClass());
        SLF4JLoggerProxy.info(this,
                              "{} starting",
                              serviceName);
        clusterData = clusterService.getInstanceData();
        strategyIncomingDirectoryName = strategyIncomingDirectoryName + clusterData.getInstanceNumber();
        incomingStrategyDirectoryPath = Paths.get(strategyIncomingDirectoryName);
        // intentionally not modified with the cluster instance number; it's ok if mulitple instances use this directory
        temporaryStrategyDirectoryPath = Paths.get(strategyTemporaryDirectoryName);
        strategyWatcher = new DirectoryWatcherImpl();
        strategyWatcher.setCreateDirectoriesOnStart(true);
        strategyWatcher.setDirectoriesToWatch(Lists.newArrayList(new File(strategyIncomingDirectoryName)));
        strategyWatcher.setPollingInterval(pollingInterval);
        strategyWatcher.addWatcher(this);
        strategyWatcher.start();
        SLF4JLoggerProxy.info(this,
                              "{} watching {} for incoming strategies",
                              serviceName,
                              strategyIncomingDirectoryName);
    }
    /**
     * Stop the object.
     */
    @PreDestroy
    public void stop()
    {
        if(strategyWatcher != null) {
            try {
                strategyWatcher.stop();
            } catch (Exception ignored) {
            } finally {
                strategyWatcher = null;
            }
        }
        SLF4JLoggerProxy.info(this,
                              "{} stopped",
                              serviceName);
    }
    /**
     * Requests loaded strategy instances.
     *
     * @returns a <code>Collection<StrategyInstance></code> value
     */
    @Override
    @Transactional(readOnly=true,propagation=Propagation.REQUIRED)
    public Collection<? extends StrategyInstance> getStrategyInstances(String inCurrentUserName)
    {
        // TODO need to filter by current user
        // TODO probably need to factor in supervisor permissions for "read"
        return strategyInstanceDao.findAll();
    }
    /**
     * Unload a strategy instance.
     *
     * @param inStrategyInstanceName a <code>String</code> value
     */
    @Override
    @Transactional(readOnly=false,propagation=Propagation.REQUIRED)
    public void unloadStrategyInstance(String inStrategyInstanceName)
    {
        Validate.notNull(inStrategyInstanceName,
                         "Strategy instance name required");
        Optional<PersistentStrategyInstance> strategyInstanceOption = strategyInstanceDao.findByName(inStrategyInstanceName);
        Validate.isTrue(strategyInstanceOption.isPresent(),
                        "No strategy instance by name '" + inStrategyInstanceName + "'");
        PersistentStrategyInstance strategyInstance = strategyInstanceOption.get();
        Validate.isTrue(strategyInstance.getStatus().isUnloadable(),
                        "Strategy '" + strategyInstance.getName() + "' cannot be unloaded at status '" + strategyInstance.getStatus() + "'");
        // TODO need to put the correct filename in here
//        FileUtils.deleteQuietly(new File(strategyInstance.getFilename()));
        strategyInstanceDao.delete(strategyInstance);
    }
    /* (non-Javadoc)
     * @see StrategyService#getIncomingStrategyDirectory()
     */
    @Override
    public Path getIncomingStrategyDirectory()
    {
        return incomingStrategyDirectoryPath;
    }
    /* (non-Javadoc)
     * @see StrategyService#getTemporaryStrategyDirectory()
     */
    @Override
    public Path getTemporaryStrategyDirectory()
    {
        return temporaryStrategyDirectoryPath;
    }
    /**
     * Load a new strategy instances.
     *
     * @param inStrategyInstance an <code>StrategyInstance</code> value
     * @returns an <code>StrategyStatus</code> value
     */
    @Override
    @Transactional(readOnly=false,propagation=Propagation.REQUIRED)
    public void received(File inFile,
                         String inOriginalFileName)
    {
        SLF4JLoggerProxy.debug(this,
                               "Received incoming strategy file '{}'",
                               inOriginalFileName);
        // TODO match with strategy instance and update status to LOADED or ERROR
        // verify and move to provisioning directory
        // find the incoming upload
//        Optional<? extends StrategyInstance> strategyInstanceOption = strategyService.findByName(inName);
//        Validate.isTrue(strategyInstanceOption.isPresent(),
//                        "No strategy instance with name '" + inName + "' found");
//        StrategyInstance strategyInstance = strategyInstanceOption.get();
//        Validate.isTrue(inNonce.equals(strategyInstance.getNonce()),
//                        "Strategy upload nonce does not match");
//        String hash = PlatformServices.getFileChecksum(inStrategyFile.toFile());
//        Validate.isTrue(hash.equals(strategyInstance.getHash()),
//                        "Strategy upload hash does not match");
        PersistentStrategyInstance strategyInstance = null;
        try {
            String nonce = FilenameUtils.getBaseName(inOriginalFileName);
            Optional<PersistentStrategyInstance> strategyInstanceOption = strategyInstanceDao.findByNonce(nonce);
            SLF4JLoggerProxy.debug(this,
                                   "Received uploaded file with nonce: '{}' found: {}",
                                   nonce,
                                   strategyInstanceOption);
            Validate.isTrue(strategyInstanceOption.isPresent(),
                            "No strategy instance with nonce: '" + nonce + "'");
            strategyInstance = strategyInstanceOption.get();
            // TODO need to move file to a storage directory
            // TODO need to rename file to use the nonce
//            FileUtils.moveFileToDirectory(inFile,
//                                          Paths.get(provisioningAgent.getProvisioningDirectory()).toFile(),
//                                          false);
            strategyInstance.setStatus(StrategyStatus.STOPPED);
            strategyInstance = strategyInstanceDao.save(strategyInstance);
            // TODO send strategy load event
        } catch (Exception e) {
            SLF4JLoggerProxy.warn(this,
                                  e);
            if(strategyInstance != null) {
                strategyInstance.setStatus(StrategyStatus.ERROR);
                strategyInstance = strategyInstanceDao.save(strategyInstance);
            }
            // TODO send strategy load failed event
        }
    }
    /**
     * Load a new strategy instances.
     *
     * @param inStrategyInstance an <code>StrategyInstance</code> value
     * @returns an <code>StrategyStatus</code> value
     */
    @Override
    @Transactional(readOnly=false,propagation=Propagation.REQUIRED)
    public StrategyStatus loadStrategyInstance(StrategyInstance inStrategyInstance)
    {
        // create a new persistent strategy instance
        PersistentStrategyInstance pInstance;
        if(inStrategyInstance instanceof PersistentStrategyInstance) {
            pInstance = (PersistentStrategyInstance)inStrategyInstance;
        } else {
            throw new UnsupportedOperationException("Need to create persistent instance");
        }
        pInstance.setStatus(StrategyStatus.LOADING);
        PersistentUser user = userDao.findByName(inStrategyInstance.getUser().getName());
        Validate.notNull(user,
                         "No user for name '" + inStrategyInstance.getUser().getName() + "'");
        pInstance.setUser(user);
        pInstance = strategyInstanceDao.save(pInstance);
        return pInstance.getStatus();
    }
    /**
     * Finds the strategy instance with the given name.
     *
     * @param inName a <code>String</code> value
     * @returns a <code>Optional<? extends StrategyInstance></code> value
     */
    @Override
    @Transactional(readOnly=true,propagation=Propagation.REQUIRED)
    public Optional<? extends StrategyInstance> findByName(String inName)
    {
        return strategyInstanceDao.findByName(inName);
    }
    private Path incomingStrategyDirectoryPath;
    private Path temporaryStrategyDirectoryPath;
    /**
     * interval at which to poll for provisioning files
     */
    @Value("${metc.strategy.incoming.directory.polling.intervalms:5000}")
    private long pollingInterval;
    /**
     * strategy incoming directory base
     */
    @Value("${metc.strategy.incoming.directory}")
    private String strategyIncomingDirectoryName;
    /**
     * strategy temporary directory base
     */
    @Value("${metc.strategy.temporary.directory}")
    private String strategyTemporaryDirectoryName;
    private String serviceName;
    private DirectoryWatcherImpl strategyWatcher;
    /**
     * provides access to cluster services
     */
    @Autowired
    private ClusterService clusterService;
    /**
     * generated cluster data
     */
    private ClusterData clusterData;
    /**
     * provides access to the {@link User} data store
     */
    @Autowired
    private UserDao userDao;
    /**
     * provides access to the {@link StrategyInstance} data store
     */
    @Autowired
    private StrategyInstanceDao strategyInstanceDao;
    /**
     * provides access to provisioning services
     */
    @Autowired
    private ProvisioningAgent provisioningAgent;
}
